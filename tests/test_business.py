"""业务规则测试：注册积分、对话扣费、灵感刷新、402、关键词上限、级联删除。"""

from __future__ import annotations

import httpx
import pytest
from sqlalchemy import func, select, update

from app.models.balance import Balance
from app.models.conversation import Conversation
from app.models.inspiration import InspirationKeyword, InspirationVideo
from app.models.message import Message
from app.models.user import User
from tests.ai_mock import install_fake_ai
from tests.conftest import (
    AGENT_PUBLIC_ID,
    SEED_BALANCE,
    SEED_CONVERSATION_ID,
    SEED_KEYWORD_ID,
    SEED_USER_ID,
    assert_error,
    assert_ok,
    bearer,
    read_sse,
    register,
)


async def set_balance(session, user_id: int, credits: int) -> None:
    await session.execute(
        update(Balance).where(Balance.user_id == user_id).values(token_credits=credits)
    )
    await session.commit()


async def get_balance(session, user_id: int = SEED_USER_ID) -> int:
    value = await session.scalar(select(Balance.token_credits).where(Balance.user_id == user_id))
    assert value is not None
    return int(value)


async def count_messages(session, conversation_id: int) -> int:
    value = await session.scalar(
        select(func.count()).select_from(Message).where(Message.conversation_id == conversation_id)
    )
    return int(value or 0)


async def post_chat(
    client: httpx.AsyncClient,
    headers: dict[str, str],
    text: str = "你好，请介绍一下你能做什么",
    **extra: object,
) -> httpx.Response:
    payload = {"agent_id": AGENT_PUBLIC_ID, "text": text, **extra}
    return await client.post("/api/agents/chat/agents", json=payload, headers=headers)


async def test_register_grants_start_balance(client: httpx.AsyncClient, db_session) -> None:
    data = await register(client, "credits@example.com")
    balance = assert_ok(await client.get("/api/balance", headers=bearer(data["token"])))["data"]
    assert balance["tokenCredits"] == 100_000

    user = await db_session.scalar(select(User).where(User.email == "credits@example.com"))
    assert user is not None
    assert await get_balance(db_session, user.id) == 100_000


async def test_chat_deducts_ten_credits(
    client: httpx.AsyncClient,
    seed_headers: dict[str, str],
    db_session,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    install_fake_ai(monkeypatch)

    response = await post_chat(client, seed_headers)
    assert response.status_code == 200
    frames = await read_sse(response)
    assert frames[-1][0] == "final"

    assert await get_balance(db_session) == SEED_BALANCE - 10


async def test_chat_never_goes_negative(
    client: httpx.AsyncClient,
    seed_headers: dict[str, str],
    db_session,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    install_fake_ai(monkeypatch)
    await set_balance(db_session, SEED_USER_ID, 5)

    response = await post_chat(client, seed_headers)
    assert response.status_code == 200
    await read_sse(response)

    assert await get_balance(db_session) == 0


async def test_inspiration_refresh_deducts_sixty_credits(
    client: httpx.AsyncClient, seed_headers: dict[str, str], db_session
) -> None:
    keyword = await db_session.get(InspirationKeyword, SEED_KEYWORD_ID)
    assert keyword is not None
    assert keyword.cache_expires_at is None

    assert_ok(
        await client.post(
            f"/api/inspiration/keywords/{SEED_KEYWORD_ID}/refresh", headers=seed_headers
        )
    )

    await db_session.refresh(keyword)
    assert keyword.cache_expires_at is not None
    assert await get_balance(db_session) == SEED_BALANCE - 60


async def test_inspiration_refresh_insufficient_credits_returns_402(
    client: httpx.AsyncClient, seed_headers: dict[str, str], db_session
) -> None:
    await set_balance(db_session, SEED_USER_ID, 59)

    response = await client.post(
        f"/api/inspiration/keywords/{SEED_KEYWORD_ID}/refresh", headers=seed_headers
    )
    assert_error(response, 402, "积分不足，刷新需要60积分")

    # 扣费与续期都没有发生
    assert await get_balance(db_session) == 59
    keyword = await db_session.get(InspirationKeyword, SEED_KEYWORD_ID)
    assert keyword is not None
    assert keyword.cache_expires_at is None


async def test_inspiration_refresh_with_exact_balance_succeeds(
    client: httpx.AsyncClient, seed_headers: dict[str, str], db_session
) -> None:
    await set_balance(db_session, SEED_USER_ID, 60)
    assert_ok(
        await client.post(
            f"/api/inspiration/keywords/{SEED_KEYWORD_ID}/refresh", headers=seed_headers
        )
    )
    assert await get_balance(db_session) == 0


async def test_keyword_limit_is_five(
    client: httpx.AsyncClient, seed_headers: dict[str, str], db_session
) -> None:
    for index in range(4):
        assert_ok(
            await client.post(
                "/api/inspiration/keywords",
                json={"keyword": f"关键词{index}"},
                headers=seed_headers,
            )
        )

    count = await db_session.scalar(
        select(func.count())
        .select_from(InspirationKeyword)
        .where(InspirationKeyword.user_id == SEED_USER_ID)
    )
    assert count == 5

    response = await client.post(
        "/api/inspiration/keywords",
        json={"keyword": "第六个"},
        headers=seed_headers,
    )
    assert_error(response, 400, "最多5个关键词")


async def test_keyword_duplicate_is_conflict(
    client: httpx.AsyncClient, seed_headers: dict[str, str]
) -> None:
    response = await client.post(
        "/api/inspiration/keywords",
        json={"keyword": "Aigc培训"},
        headers=seed_headers,
    )
    assert_error(response, 409, "关键词已存在")


async def test_disabled_user_cannot_login(client: httpx.AsyncClient, db_session) -> None:
    await db_session.execute(update(User).where(User.id == SEED_USER_ID).values(disabled=True))
    await db_session.commit()

    response = await client.post(
        "/api/auth/login", json={"email": "test@xianting.cn", "password": "123456"}
    )
    assert_error(response, 403, "账号已禁用")


async def test_delete_conversation_cascades_messages(
    client: httpx.AsyncClient, seed_headers: dict[str, str], db_session
) -> None:
    conversation = await db_session.scalar(
        select(Conversation).where(Conversation.public_id == SEED_CONVERSATION_ID)
    )
    assert conversation is not None
    conversation_id = conversation.id
    assert await count_messages(db_session, conversation_id) == 2

    assert_ok(await client.delete(f"/api/convos/{SEED_CONVERSATION_ID}", headers=seed_headers))

    db_session.expire_all()
    assert await db_session.get(Conversation, conversation_id) is None
    assert await count_messages(db_session, conversation_id) == 0


async def test_cross_user_conversation_id_creates_new_conversation(
    client: httpx.AsyncClient,
    seed_headers: dict[str, str],
    db_session,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """B 用户传入 A 的 conversationId 时，不能读到 A 的历史，而是新建 B 的会话。"""

    calls = install_fake_ai(monkeypatch)

    other = await register(client, "cross-chat@example.com")
    headers = bearer(other["token"])

    response = await post_chat(
        client,
        headers,
        text="B 用户的消息",
        conversationId=SEED_CONVERSATION_ID,
    )
    assert response.status_code == 200
    frames = await read_sse(response)
    assert [name for name, _ in frames] == [
        "conversation",
        "message",
        "message",
        "message",
        "final",
    ]

    new_conversation_id = frames[0][1]["conversationId"]
    assert new_conversation_id != SEED_CONVERSATION_ID
    # 第一次对话没有历史（不会带入 A 的消息）
    assert calls[0]["history"] == []

    mine = assert_ok(await client.get("/api/convos", headers=headers))["data"]
    assert [c["_id"] for c in mine["conversations"]] == [new_conversation_id]

    owner = assert_ok(await client.get("/api/convos", headers=seed_headers))["data"]
    assert len(owner["conversations"]) == 2
    assert SEED_CONVERSATION_ID in [c["_id"] for c in owner["conversations"]]


async def test_deleting_keyword_removes_videos(
    client: httpx.AsyncClient, seed_headers: dict[str, str], db_session
) -> None:
    videos = await db_session.scalar(
        select(func.count())
        .select_from(InspirationVideo)
        .where(InspirationVideo.keyword_id == SEED_KEYWORD_ID)
    )
    assert videos == 10

    assert_ok(
        await client.delete(f"/api/inspiration/keywords/{SEED_KEYWORD_ID}", headers=seed_headers)
    )

    remaining = await db_session.scalar(
        select(func.count())
        .select_from(InspirationVideo)
        .where(InspirationVideo.keyword_id == SEED_KEYWORD_ID)
    )
    assert remaining == 0
