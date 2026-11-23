"""契约测试：以 Java DTO 定义生成的 golden snapshot 校验响应字段集合。

Java 运行环境不可用（Java 14 / 无 Maven / MySQL 未启动），按测试计划约定
以 ``tests/golden/java_dto_shapes.json``（源自 Java DTO 源码）为准。
"""

from __future__ import annotations

import json
from pathlib import Path

import httpx

from tests.conftest import (
    AGENT_PUBLIC_ID,
    SEED_CONVERSATION_ID,
    SEED_KEYWORD_ID,
    assert_ok,
)

GOLDEN = json.loads(
    Path(__file__).with_name("golden").joinpath("java_dto_shapes.json").read_text("utf-8")
)


def assert_fields(payload: dict, dto_name: str, omitted: set[str] | None = None) -> None:
    expected = set(GOLDEN["dto"][dto_name]) - (omitted or set())
    assert set(payload) == expected, f"{dto_name}: {sorted(set(payload) ^ expected)}"


def test_envelope_keys() -> None:
    assert GOLDEN["envelope"] == ["code", "message", "data", "timestamp"]
    assert GOLDEN["page_result"] == [
        "object",
        "data",
        "first_id",
        "last_id",
        "has_more",
        "after",
    ]


async def test_auth_response_contract(client: httpx.AsyncClient) -> None:
    body = assert_ok(
        await client.post(
            "/api/auth/login", json={"email": "test@xianting.cn", "password": "123456"}
        )
    )
    assert set(body) == set(GOLDEN["envelope"])
    assert set(body["data"]) == set(GOLDEN["auth_response"])
    assert_fields(body["data"]["user"], "UserDto", omitted={"avatar"})


async def test_user_contract(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/user", headers=seed_headers))["data"]
    assert_fields(data, "UserDto", omitted={"avatar"})


async def test_agent_contract(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get(f"/api/agents/{AGENT_PUBLIC_ID}", headers=seed_headers))[
        "data"
    ]
    assert_fields(data, "AgentDto", omitted={"avatar"})

    page = assert_ok(await client.get("/api/agents", headers=seed_headers))["data"]
    assert set(page) == set(GOLDEN["page_result"]) - {"after"}
    for agent in page["data"]:
        assert_fields(agent, "AgentDto", omitted={"avatar"})


async def test_department_contract(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/departments", headers=seed_headers))["data"]
    for department in data:
        assert_fields(department, "DepartmentDto", omitted={"extraTools"})
        assert department["__v"] == 0


async def test_balance_contract(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/balance", headers=seed_headers))["data"]
    assert_fields(data, "BalanceDto")


async def test_conversation_contract(
    client: httpx.AsyncClient, seed_headers: dict[str, str]
) -> None:
    data = assert_ok(await client.get("/api/convos", headers=seed_headers))["data"]
    for conversation in data["conversations"]:
        assert_fields(conversation, "ConversationDto", omitted={"agent_id"})


async def test_message_contract(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(
        await client.get(f"/api/convos/{SEED_CONVERSATION_ID}/messages", headers=seed_headers)
    )["data"]
    user_message = next(message for message in data if message["sender"] == "user")
    agent_message = next(message for message in data if message["sender"] == "agent")
    assert_fields(user_message, "MessageDto", omitted={"parentMessageId"})
    assert_fields(agent_message, "MessageDto")


async def test_inspiration_contract(
    client: httpx.AsyncClient, seed_headers: dict[str, str]
) -> None:
    data = assert_ok(await client.get("/api/inspiration", headers=seed_headers))["data"]
    keyword = data["data"]["keywords"][0]
    assert_fields(keyword, "InspirationKeywordDto", omitted={"cacheExpiresAt"})
    for video in keyword["videos"]:
        assert_fields(video, "InspirationVideoDto", omitted={"coverUrl", "videoUrl"})

    refreshed = assert_ok(
        await client.post(
            f"/api/inspiration/keywords/{SEED_KEYWORD_ID}/refresh", headers=seed_headers
        )
    )["data"]
    assert_fields(refreshed, "InspirationKeywordDto")
