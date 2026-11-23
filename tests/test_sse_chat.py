"""SSE 对话测试：mock DeepSeek 流，验证事件名、顺序、JSON 字段与落库。"""

from __future__ import annotations

from typing import Any

import httpx
import pytest
from sqlalchemy import func, select

from app.db.session import SessionLocal
from app.models.balance import Balance
from app.models.conversation import Conversation
from app.models.message import Message
from app.services.chat import ChatPayload, ChatService, sse_event
from tests.ai_mock import install_failing_ai, install_fake_ai
from tests.conftest import (
    AGENT_PUBLIC_ID,
    SEED_BALANCE,
    SEED_USER_ID,
    assert_error,
    parse_sse,
    read_sse,
)


async def post_chat(
    client: httpx.AsyncClient,
    headers: dict[str, str],
    text: str = "你好，请介绍一下你能做什么",
    **extra: Any,
) -> httpx.Response:
    return await client.post(
        "/api/agents/chat/agents",
        json={"agent_id": AGENT_PUBLIC_ID, "text": text, **extra},
        headers=headers,
    )


async def messages_of(db_session, public_id: str) -> list[Message]:
    conversation = await db_session.scalar(
        select(Conversation).where(Conversation.public_id == public_id)
    )
    assert conversation is not None
    rows = await db_session.scalars(
        select(Message).where(Message.conversation_id == conversation.id).order_by(Message.id)
    )
    return list(rows)


async def test_sse_frame_format() -> None:
    frame = sse_event("message", {"text": "你好", "n": 1})
    assert frame == 'event:message\ndata:{"text":"你好","n":1}\n\n'
    # 每个事件以空行结束，前端按 \n\n 切帧
    assert frame.endswith("\n\n")


async def test_new_conversation_streams_and_persists(
    client: httpx.AsyncClient,
    seed_headers: dict[str, str],
    db_session,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    calls = install_fake_ai(monkeypatch, pieces=("你好", "，", "世界"))
    text = "这是一段超过二十个字的用户消息用于验证会话标题截断逻辑"

    response = await post_chat(client, seed_headers, text=text, messageId="client-message-1")
    assert response.status_code == 200
    assert response.headers["content-type"].startswith("text/event-stream")
    assert response.headers["cache-control"] == "no-cache"
    assert response.headers["x-accel-buffering"] == "no"

    frames = await read_sse(response)
    assert [name for name, _ in frames] == [
        "conversation",
        "message",
        "message",
        "message",
        "final",
    ]

    conversation, final = frames[0][1], frames[-1][1]
    assert set(conversation) == {
        "_id",
        "conversationId",
        "title",
        "endpoint",
        "createdAt",
        "updatedAt",
        "agent_id",
    }
    assert conversation["_id"] == conversation["conversationId"]
    assert conversation["title"] == text[:20]
    assert conversation["endpoint"] == "agents"
    assert conversation["agent_id"] == AGENT_PUBLIC_ID

    chunks = [data for name, data in frames if name == "message"]
    assert "".join(chunk["text"] for chunk in chunks) == "你好，世界"
    assert all(chunk["conversationId"] == conversation["conversationId"] for chunk in chunks)
    assert all(chunk["messageId"] == final["messageId"] for chunk in chunks)
    assert all(set(chunk) == {"messageId", "conversationId", "text"} for chunk in chunks)

    assert set(final) == {
        "_id",
        "messageId",
        "conversationId",
        "parentMessageId",
        "sender",
        "isCreatedByUser",
        "text",
        "error",
        "createdAt",
    }
    assert final["_id"] == final["messageId"]
    assert final["conversationId"] == conversation["conversationId"]
    assert final["parentMessageId"] == "client-message-1"
    assert final["sender"] == "agent"
    assert final["isCreatedByUser"] is False
    assert final["text"] == "你好，世界"
    assert final["error"] is False
    assert final["createdAt"]

    # 落库：1 条用户消息 + 1 条助手消息；标题与 agent 外键正确
    rows = await messages_of(db_session, conversation["conversationId"])
    assert len(rows) == 2
    assert rows[0].public_id == "client-message-1"
    assert rows[0].sender == "user"
    assert rows[0].text == text
    assert rows[0].parent_message_id is None
    assert rows[1].sender == "agent"
    assert rows[1].parent_message_id == "client-message-1"
    assert rows[1].text == "你好，世界"
    assert rows[1].public_id == final["messageId"]

    conversation_row = await db_session.scalar(
        select(Conversation).where(Conversation.public_id == conversation["conversationId"])
    )
    assert conversation_row is not None
    assert conversation_row.user_id == SEED_USER_ID
    assert conversation_row.title == text[:20]
    assert conversation_row.updated_at >= conversation_row.created_at

    balance = (await client.get("/api/balance", headers=seed_headers)).json()["data"][
        "tokenCredits"
    ]
    assert balance == SEED_BALANCE - 10

    # 第一次调用不带历史
    assert calls[0]["history"] == []
    assert calls[0]["user_text"] == text
    assert calls[0]["agent"] == "爆款选题助手"


async def test_reuse_conversation_and_history_window(
    client: httpx.AsyncClient,
    seed_headers: dict[str, str],
    db_session,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    calls = install_fake_ai(monkeypatch)

    first = await post_chat(client, seed_headers, text="第一轮")
    first_frames = await read_sse(first)
    conversation_id = first_frames[0][1]["conversationId"]

    second = await post_chat(client, seed_headers, text="第二轮", conversationId=conversation_id)
    second_frames = await read_sse(second)

    assert [name for name, _ in second_frames] == ["message", "message", "message", "final"]
    assert second_frames[-1][1]["conversationId"] == conversation_id

    # 第二轮把第一轮的 user + agent 消息带入 prompt，且不含本轮用户消息
    assert calls[1]["history"] == [("user", "第一轮"), ("agent", "你好，世界")]

    rows = await messages_of(db_session, conversation_id)
    assert [m.sender for m in rows] == ["user", "agent", "user", "agent"]
    assert [m.text for m in rows] == ["第一轮", "你好，世界", "第二轮", "你好，世界"]

    balance = (await client.get("/api/balance", headers=seed_headers)).json()["data"][
        "tokenCredits"
    ]
    assert balance == SEED_BALANCE - 20


async def test_short_title_is_not_padded(
    client: httpx.AsyncClient,
    seed_headers: dict[str, str],
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    install_fake_ai(monkeypatch)
    frames = await read_sse(await post_chat(client, seed_headers, text="短标题"))
    assert frames[0][1]["title"] == "短标题"


async def test_upstream_error_emits_error_event_and_keeps_user_message(
    client: httpx.AsyncClient,
    seed_headers: dict[str, str],
    db_session,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    install_failing_ai(monkeypatch, message="模型调用失败", before="半句")

    frames = await read_sse(await post_chat(client, seed_headers, text="会失败的问题"))
    assert [name for name, _ in frames] == ["conversation", "message", "error"]
    assert frames[-1][1] == {"message": "模型调用失败"}

    conversation_id = frames[0][1]["conversationId"]
    rows = await messages_of(db_session, conversation_id)
    assert len(rows) == 1  # 助手消息不落库
    assert rows[0].sender == "user"

    balance = (await client.get("/api/balance", headers=seed_headers)).json()["data"][
        "tokenCredits"
    ]
    assert balance == SEED_BALANCE - 10  # 已扣积分不退还


async def test_missing_api_key_returns_json_error_before_stream(
    client: httpx.AsyncClient,
    seed_headers: dict[str, str],
    db_session,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    install_fake_ai(monkeypatch)
    monkeypatch.setattr("app.core.config.settings.deepseek_api_key", "")

    response = await post_chat(client, seed_headers)
    assert_error(response, 500)
    assert "DEEPSEEK_API_KEY" in response.json()["message"]

    balance = (await client.get("/api/balance", headers=seed_headers)).json()["data"][
        "tokenCredits"
    ]
    assert balance == SEED_BALANCE  # 未扣费
    conversations = await db_session.scalar(
        select(func.count()).select_from(Conversation).where(Conversation.user_id == 1)
    )
    assert conversations == 2  # 种子会话仍在，且没有新建


async def test_chat_validation_errors(
    client: httpx.AsyncClient,
    seed_headers: dict[str, str],
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    install_fake_ai(monkeypatch)

    assert_error(
        await client.post("/api/agents/chat/agents", json={"text": "hi"}, headers=seed_headers),
        400,
        "agent_id and text are required",
    )
    assert_error(
        await client.post(
            "/api/agents/chat/agents", json={"agent_id": AGENT_PUBLIC_ID}, headers=seed_headers
        ),
        400,
        "agent_id and text are required",
    )
    assert_error(
        await post_chat(client, seed_headers, text="   "),
        400,
        "agent_id and text are required",
    )
    assert_error(
        await client.post(
            "/api/agents/chat/agents",
            json={"agent_id": "not-exist", "text": "hi"},
            headers=seed_headers,
        ),
        404,
        "agent not found: not-exist",
    )


async def test_client_message_and_parent_ids_are_preserved(
    client: httpx.AsyncClient,
    seed_headers: dict[str, str],
    db_session,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    install_fake_ai(monkeypatch)
    frames = await read_sse(
        await post_chat(
            client,
            seed_headers,
            text="带 parent 的消息",
            messageId="client-msg",
            parentMessageId="client-parent",
        )
    )
    conversation_id = frames[0][1]["conversationId"]
    rows = await messages_of(db_session, conversation_id)
    assert rows[0].public_id == "client-msg"
    assert rows[0].parent_message_id == "client-parent"
    assert frames[-1][1]["parentMessageId"] == "client-msg"


async def test_disconnect_stops_stream_without_persisting_assistant(
    db_session, monkeypatch: pytest.MonkeyPatch
) -> None:
    """客户端断开：停止消费上游、不发 final、不写助手消息。"""

    class StubRequest:
        async def is_disconnected(self) -> bool:
            return True

    class FakeAi:
        async def stream_reply(self, agent, user_text, history):
            yield "不应被消费的片段"

    service = ChatService(session_factory=SessionLocal, ai_service=FakeAi())
    agent = await service.require_agent(AGENT_PUBLIC_ID)
    payload = ChatPayload(agent_id=AGENT_PUBLIC_ID, text="断开测试")

    frames = [
        frame async for frame in service.stream_chat(SEED_USER_ID, agent, payload, StubRequest())
    ]
    assert len(frames) == 1
    assert frames[0].startswith("event:conversation\n")

    ((_, conversation_data),) = parse_sse(frames[0])
    conversation_id = conversation_data["conversationId"]
    rows = await messages_of(db_session, conversation_id)
    assert len(rows) == 1
    assert rows[0].sender == "user"

    credits = await db_session.scalar(
        select(Balance.token_credits).where(Balance.user_id == SEED_USER_ID)
    )
    assert credits == SEED_BALANCE - 10
