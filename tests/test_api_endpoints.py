"""接口测试：25 个 REST 端点的状态码与字段形状（外加 /health）。

对应用户给出的契约：HTTP 状态码真实语义（401/402/404），响应外壳
``{code, message, data, timestamp}`` 与 Java `Result` 一致，null 字段不序列化。
"""

from __future__ import annotations

import httpx

from tests.conftest import (
    AGENT_NAME,
    AGENT_PUBLIC_ID,
    SEED_AGENT_COUNT,
    SEED_BALANCE,
    SEED_CONVERSATION_ID,
    SEED_CONVERSATION_ID_2,
    SEED_DEPARTMENT_COUNT,
    SEED_KEYWORD_ID,
    SEED_KEYWORD_TEXT,
    SEED_USER_PUBLIC_ID,
    SEED_VIDEO_COUNT,
    assert_error,
    assert_ok,
    bearer,
)

TIMESTAMP_FIELDS = {"createdAt", "updatedAt", "expireAt", "cacheExpiresAt", "contractReadAt"}


# ---------------------------------------------------------------------------
# 公开端点
# ---------------------------------------------------------------------------


async def test_health(client: httpx.AsyncClient) -> None:
    response = await client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


async def test_login(client: httpx.AsyncClient) -> None:
    body = assert_ok(
        await client.post(
            "/api/auth/login",
            json={"email": " TEST@xianting.cn ", "password": "123456", "brand": "xianting"},
        )
    )
    data = body["data"]
    assert set(data) == {"token", "user"}
    assert data["user"]["_id"] == SEED_USER_PUBLIC_ID
    assert data["user"]["email"] == "test@xianting.cn"
    assert data["user"]["emailVerified"] is True
    assert data["user"]["disabled"] is False
    assert data["user"]["contractReadAt"] is not None
    assert "avatar" not in data["user"]
    assert "password" not in data["user"]
    assert data["token"].count(".") == 1


async def test_register(client: httpx.AsyncClient) -> None:
    body = assert_ok(
        await client.post(
            "/api/auth/register",
            json={"email": "NewUser@Example.com", "password": "abcdef", "name": "新人"},
        )
    )
    data = body["data"]
    assert data["user"]["email"] == "newuser@example.com"  # 归一化：去空白 + 小写
    assert data["user"]["name"] == "新人"
    assert data["user"]["brand"] == "xianting"
    assert data["user"]["emailVerified"] is False
    assert "contractReadAt" not in data["user"]

    balance = assert_ok(await client.get("/api/balance", headers=bearer(data["token"])))
    assert balance["data"]["tokenCredits"] == 100_000
    assert balance["data"]["autoRefillEnabled"] is True
    assert balance["data"]["user"] == data["user"]["_id"]


async def test_register_duplicate(client: httpx.AsyncClient) -> None:
    response = await client.post(
        "/api/auth/register",
        json={"email": "test@xianting.cn", "password": "123456"},
    )
    assert_error(response, 409, "该邮箱已注册")


async def test_register_short_password(client: httpx.AsyncClient) -> None:
    response = await client.post(
        "/api/auth/register", json={"email": "short@example.com", "password": "12345"}
    )
    assert_error(response, 400, "邮箱必填，密码至少6位")


async def test_login_wrong_password(client: httpx.AsyncClient) -> None:
    response = await client.post(
        "/api/auth/login", json={"email": "test@xianting.cn", "password": "bad"}
    )
    assert_error(response, 401, "邮箱或密码错误")


async def test_login_missing_fields(client: httpx.AsyncClient) -> None:
    response = await client.post("/api/auth/login", json={})
    assert_error(response, 400, "email and password are required")


async def test_config_is_public(client: httpx.AsyncClient) -> None:
    body = assert_ok(await client.get("/api/config"))
    assert body["data"] == {
        "appTitle": "先t霆AI超级系统",
        "registrationEnabled": True,
        "emailLoginEnabled": True,
        "socialLogins": [],
        "ldap": {"enabled": False},
        "balance": {"enabled": True, "autoRefillEnabled": True, "startBalance": 100000},
        "conversationImportMaxFileSize": "20mb",
    }


# ---------------------------------------------------------------------------
# 认证相关（需 token）
# ---------------------------------------------------------------------------


async def test_refresh(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    body = assert_ok(await client.post("/api/auth/refresh", headers=seed_headers))
    assert set(body["data"]) == {"token"}
    assert body["data"]["token"].count(".") == 1


async def test_logout(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    body = assert_ok(await client.post("/api/auth/logout", headers=seed_headers))
    assert "data" not in body  # Java Result.ok(null) + non_null


# ---------------------------------------------------------------------------
# CoreController
# ---------------------------------------------------------------------------


async def test_user(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/user", headers=seed_headers))["data"]
    assert data["_id"] == SEED_USER_PUBLIC_ID
    assert data["brand"] == "xianting"
    assert data["email"] == "test@xianting.cn"
    assert data["name"] == "测试用户"
    assert data["emailVerified"] is True
    assert data["disabled"] is False
    assert set(data) == {
        "_id",
        "brand",
        "email",
        "name",
        "emailVerified",
        "disabled",
        "contractReadAt",
        "createdAt",
    }


async def test_balance(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/balance", headers=seed_headers))["data"]
    assert set(data) == {"tokenCredits", "autoRefillEnabled", "expireAt", "user"}
    assert data["tokenCredits"] == SEED_BALANCE
    assert data["autoRefillEnabled"] is True
    assert data["user"] == SEED_USER_PUBLIC_ID


async def test_notifications(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/notifications", headers=seed_headers))["data"]
    assert data == {"items": [], "total": 0, "preferences": {"muted": False}}


async def test_roles(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/roles/user", headers=seed_headers))["data"]
    assert data == {
        "role": "USER",
        "name": "User",
        "permissions": {"AGENTS": {"CREATE": True, "SHARE": False, "SHARE_PUBLIC": False}},
    }


async def test_models(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/models", headers=seed_headers))["data"]
    assert data == {
        "agents": ["先霆默认模型"],
        "DeepSeek": ["deepseek-chat", "deepseek-reasoner"],
    }


async def test_search_enable(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/search/enable", headers=seed_headers))["data"]
    assert data is False


async def test_endpoints(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/endpoints", headers=seed_headers))["data"]
    assert data == {"agents": True}


async def test_banner(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/banner", headers=seed_headers))["data"]
    assert data == {"show": False}


# ---------------------------------------------------------------------------
# AgentController
# ---------------------------------------------------------------------------


async def test_departments(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/departments", headers=seed_headers))["data"]
    assert len(data) == SEED_DEPARTMENT_COUNT
    assert [d["sortOrder"] for d in data] == list(range(SEED_DEPARTMENT_COUNT))
    first = data[0]
    assert set(first) == {"_id", "name", "enabled", "brandOnly", "sortOrder", "__v"}
    assert first["name"] == "流量部门"
    assert first["__v"] == 0
    assert "extraTools" not in first


async def test_agents_list(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/agents", headers=seed_headers))["data"]
    assert data["object"] == "list"
    assert data["has_more"] is False
    assert len(data["data"]) == SEED_AGENT_COUNT
    assert data["first_id"] == data["data"][0]["_id"]
    assert data["last_id"] == data["data"][-1]["_id"]
    assert "after" not in data

    agent = next(item for item in data["data"] if item["_id"] == AGENT_PUBLIC_ID)
    assert agent["agent_id"] == agent["_id"]
    assert agent["name"] == AGENT_NAME
    assert agent["department"] == "流量部门"
    assert agent["isPreset"] is True
    assert isinstance(agent["conversation_starters"], list)
    assert len(agent["conversation_starters"]) == 3
    assert "avatar" not in agent

    assert set(agent) <= {
        "_id",
        "agent_id",
        "name",
        "description",
        "avatar",
        "category",
        "department",
        "isPreset",
        "conversation_starters",
        "instructions",
        "createdAt",
    }


async def test_agents_filter_by_department(
    client: httpx.AsyncClient, seed_headers: dict[str, str]
) -> None:
    data = assert_ok(
        await client.get("/api/agents", params={"department": "流量部门"}, headers=seed_headers)
    )["data"]
    assert [a["name"] for a in data["data"]] == ["爆款选题助手", "标题优化大师", "热点追踪员"]

    by_public_id = assert_ok(
        await client.get(
            "/api/agents",
            params={"department": "265846bb-ba04-4d32-a17a-91e0bcaf6ece"},
            headers=seed_headers,
        )
    )["data"]
    assert len(by_public_id["data"]) == 3


async def test_agents_filter_by_is_preset(
    client: httpx.AsyncClient, seed_headers: dict[str, str]
) -> None:
    data = assert_ok(
        await client.get("/api/agents", params={"isPreset": "1"}, headers=seed_headers)
    )["data"]
    assert len(data["data"]) == SEED_AGENT_COUNT


async def test_agents_unknown_department(
    client: httpx.AsyncClient, seed_headers: dict[str, str]
) -> None:
    response = await client.get(
        "/api/agents", params={"department": "不存在"}, headers=seed_headers
    )
    assert_error(response, 404, "department not found: 不存在")


async def test_agents_my_access(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/agents/my-access", headers=seed_headers))["data"]
    assert len(data["data"]) == SEED_AGENT_COUNT
    assert AGENT_PUBLIC_ID in data["data"]


async def test_agents_chat_active(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/agents/chat/active", headers=seed_headers))["data"]
    assert data == []


async def test_agent_detail(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get(f"/api/agents/{AGENT_PUBLIC_ID}", headers=seed_headers))[
        "data"
    ]
    assert data["_id"] == AGENT_PUBLIC_ID
    assert data["agent_id"] == AGENT_PUBLIC_ID
    assert data["name"] == AGENT_NAME
    assert data["instructions"]
    assert data["createdAt"]


async def test_agent_detail_not_found(
    client: httpx.AsyncClient, seed_headers: dict[str, str]
) -> None:
    response = await client.get("/api/agents/not-exist", headers=seed_headers)
    assert_error(response, 404, "agent not found: not-exist")


# ---------------------------------------------------------------------------
# ConversationController
# ---------------------------------------------------------------------------


async def test_conversations_list(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/convos", headers=seed_headers))["data"]
    conversations = data["conversations"]
    assert len(conversations) == 2
    # updated_at 倒序
    assert conversations[0]["_id"] == SEED_CONVERSATION_ID_2
    assert conversations[1]["_id"] == SEED_CONVERSATION_ID
    for conversation in conversations:
        assert set(conversation) == {
            "_id",
            "conversationId",
            "title",
            "endpoint",
            "createdAt",
            "updatedAt",
        }
        assert conversation["conversationId"] == conversation["_id"]
        assert "agent_id" not in conversation


async def test_conversations_filter_by_agent(
    client: httpx.AsyncClient, seed_headers: dict[str, str]
) -> None:
    data = assert_ok(
        await client.get("/api/convos", params={"agent_id": AGENT_PUBLIC_ID}, headers=seed_headers)
    )["data"]
    assert [c["_id"] for c in data["conversations"]] == [SEED_CONVERSATION_ID]


async def test_messages(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(
        await client.get(f"/api/convos/{SEED_CONVERSATION_ID}/messages", headers=seed_headers)
    )["data"]
    assert len(data) == 2
    user_message, agent_message = data

    assert user_message["sender"] == "user"
    assert user_message["isCreatedByUser"] is True
    assert "parentMessageId" not in user_message
    assert set(user_message) == {
        "_id",
        "messageId",
        "conversationId",
        "sender",
        "isCreatedByUser",
        "text",
        "error",
        "createdAt",
    }
    assert user_message["conversationId"] == SEED_CONVERSATION_ID
    assert user_message["messageId"] == user_message["_id"]

    assert agent_message["sender"] == "agent"
    assert agent_message["isCreatedByUser"] is False
    assert agent_message["parentMessageId"] == user_message["_id"]


async def test_conversation_messages_not_found(
    client: httpx.AsyncClient, seed_headers: dict[str, str]
) -> None:
    response = await client.get("/api/convos/not-exist/messages", headers=seed_headers)
    assert_error(response, 404, "conversation not found")


async def test_rename_conversation(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(
        await client.patch(
            f"/api/convos/{SEED_CONVERSATION_ID}",
            json={"title": "  新标题  "},
            headers=seed_headers,
        )
    )["data"]
    assert data["title"] == "新标题"
    assert data["_id"] == SEED_CONVERSATION_ID
    assert "agent_id" not in data

    listed = assert_ok(await client.get("/api/convos", headers=seed_headers))["data"]
    updated = next(c for c in listed["conversations"] if c["_id"] == SEED_CONVERSATION_ID)
    assert updated["title"] == "新标题"


async def test_rename_requires_title(
    client: httpx.AsyncClient, seed_headers: dict[str, str]
) -> None:
    response = await client.patch(
        f"/api/convos/{SEED_CONVERSATION_ID}", json={"title": "   "}, headers=seed_headers
    )
    assert_error(response, 400, "title is required")


async def test_delete_conversation(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(
        await client.delete(f"/api/convos/{SEED_CONVERSATION_ID}", headers=seed_headers)
    )["data"]
    assert data == {"success": True}

    remaining = assert_ok(await client.get("/api/convos", headers=seed_headers))["data"]
    assert [c["_id"] for c in remaining["conversations"]] == [SEED_CONVERSATION_ID_2]

    response = await client.get(
        f"/api/convos/{SEED_CONVERSATION_ID}/messages", headers=seed_headers
    )
    assert_error(response, 404, "conversation not found")


# ---------------------------------------------------------------------------
# InspirationController
# ---------------------------------------------------------------------------


async def test_inspiration_list(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(await client.get("/api/inspiration", headers=seed_headers))["data"]
    assert set(data) == {"data"}
    keywords = data["data"]["keywords"]
    assert len(keywords) == 1
    keyword = keywords[0]
    assert keyword["_id"] == SEED_KEYWORD_ID
    assert keyword["keyword"] == SEED_KEYWORD_TEXT
    assert keyword["order"] == 0
    assert "cacheExpiresAt" not in keyword
    assert len(keyword["videos"]) == SEED_VIDEO_COUNT

    video = keyword["videos"][0]
    assert set(video) == {
        "_id",
        "author",
        "fans",
        "duration",
        "publishDate",
        "title",
        "likes",
        "lowFansHighLikes",
    }
    assert "coverUrl" not in video
    assert "videoUrl" not in video


async def test_add_keyword(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(
        await client.post(
            "/api/inspiration/keywords", json={"keyword": "  新关键词  "}, headers=seed_headers
        )
    )["data"]
    assert data["keyword"] == "新关键词"
    assert data["order"] == 1
    assert data["videos"] == []
    assert isinstance(data["_id"], int)
    assert data["cacheExpiresAt"]

    listed = assert_ok(await client.get("/api/inspiration", headers=seed_headers))["data"]
    assert [k["keyword"] for k in listed["data"]["keywords"]] == [
        SEED_KEYWORD_TEXT,
        "新关键词",
    ]


async def test_add_keyword_requires_value(
    client: httpx.AsyncClient, seed_headers: dict[str, str]
) -> None:
    response = await client.post(
        "/api/inspiration/keywords", json={"keyword": "   "}, headers=seed_headers
    )
    assert_error(response, 400, "keyword is required")


async def test_add_keyword_duplicate(
    client: httpx.AsyncClient, seed_headers: dict[str, str]
) -> None:
    response = await client.post(
        "/api/inspiration/keywords", json={"keyword": SEED_KEYWORD_TEXT}, headers=seed_headers
    )
    assert_error(response, 409, "关键词已存在")


async def test_refresh_keyword(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(
        await client.post(
            f"/api/inspiration/keywords/{SEED_KEYWORD_ID}/refresh", headers=seed_headers
        )
    )["data"]
    assert data["_id"] == SEED_KEYWORD_ID
    assert data["cacheExpiresAt"]
    assert len(data["videos"]) == SEED_VIDEO_COUNT

    balance = assert_ok(await client.get("/api/balance", headers=seed_headers))["data"]
    assert balance["tokenCredits"] == SEED_BALANCE - 60


async def test_refresh_keyword_not_found(
    client: httpx.AsyncClient, seed_headers: dict[str, str]
) -> None:
    response = await client.post("/api/inspiration/keywords/99999/refresh", headers=seed_headers)
    assert_error(response, 404, "keyword not found")


async def test_delete_keyword(client: httpx.AsyncClient, seed_headers: dict[str, str]) -> None:
    data = assert_ok(
        await client.delete(f"/api/inspiration/keywords/{SEED_KEYWORD_ID}", headers=seed_headers)
    )["data"]
    assert data == {"success": True}

    listed = assert_ok(await client.get("/api/inspiration", headers=seed_headers))["data"]
    assert listed["data"]["keywords"] == []


async def test_delete_keyword_not_found(
    client: httpx.AsyncClient, seed_headers: dict[str, str]
) -> None:
    response = await client.delete("/api/inspiration/keywords/99999", headers=seed_headers)
    assert_error(response, 404, "keyword not found")
