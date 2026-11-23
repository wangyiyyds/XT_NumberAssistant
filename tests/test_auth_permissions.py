"""权限测试：无 token、错误 token、过期 token、跨用户访问。

放行策略与 Java `AuthInterceptor` + `WebConfig` 一致：
只有 ``/api/auth/login``、``/api/auth/register``、``/api/config`` 与 ``/health`` 免鉴权。
"""

from __future__ import annotations

import httpx
import pytest

from app.core.config import settings
from app.core.security import issue_token
from tests.conftest import (
    AGENT_PUBLIC_ID,
    SEED_CONVERSATION_ID,
    SEED_KEYWORD_ID,
    assert_error,
    assert_ok,
    bearer,
    make_token,
    register,
)

# 所有需要鉴权的端点（方法、路径、请求体），覆盖 25 个 REST 端点中的 21 个。
PROTECTED_REQUESTS = [
    ("POST", "/api/auth/refresh", None),
    ("POST", "/api/auth/logout", None),
    ("GET", "/api/user", None),
    ("GET", "/api/balance", None),
    ("GET", "/api/notifications", None),
    ("GET", "/api/roles/user", None),
    ("GET", "/api/models", None),
    ("GET", "/api/search/enable", None),
    ("GET", "/api/endpoints", None),
    ("GET", "/api/banner", None),
    ("GET", "/api/departments", None),
    ("GET", "/api/agents", None),
    ("GET", "/api/agents/my-access", None),
    ("GET", "/api/agents/chat/active", None),
    ("GET", f"/api/agents/{AGENT_PUBLIC_ID}", None),
    ("POST", "/api/agents/chat/agents", {"agent_id": AGENT_PUBLIC_ID, "text": "你好"}),
    ("GET", "/api/convos", None),
    ("GET", f"/api/convos/{SEED_CONVERSATION_ID}/messages", None),
    ("PATCH", f"/api/convos/{SEED_CONVERSATION_ID}", {"title": "x"}),
    ("DELETE", f"/api/convos/{SEED_CONVERSATION_ID}", None),
    ("GET", "/api/inspiration", None),
    ("POST", "/api/inspiration/keywords", {"keyword": "x"}),
    ("POST", f"/api/inspiration/keywords/{SEED_KEYWORD_ID}/refresh", None),
    ("DELETE", f"/api/inspiration/keywords/{SEED_KEYWORD_ID}", None),
]


@pytest.mark.parametrize(("method", "path", "body"), PROTECTED_REQUESTS)
async def test_protected_endpoints_require_token(
    client: httpx.AsyncClient, method: str, path: str, body: dict | None
) -> None:
    response = await client.request(method, path, json=body)
    assert_error(response, 401, "invalid or expired token")


@pytest.mark.parametrize(
    "header",
    [
        {},
        {"Authorization": "abc"},
        {"Authorization": "Bearer "},
        {"Authorization": "Bearer abc"},
        {"Authorization": "Token abc"},
        {"Authorization": "bearer " + "x.y"},
        {"Authorization": "Bearer a.b.c"},
        {"Authorization": "Bearer !!!.???"},
    ],
)
async def test_malformed_authorization_is_rejected(
    client: httpx.AsyncClient, header: dict[str, str]
) -> None:
    response = await client.get("/api/user", headers=header)
    assert_error(response, 401, "invalid or expired token")


async def test_wrong_signature_is_rejected(client: httpx.AsyncClient) -> None:
    forged = issue_token(1, "another-secret", 1)
    response = await client.get("/api/user", headers=bearer(forged))
    assert_error(response, 401, "invalid or expired token")


async def test_expired_token_is_rejected(client: httpx.AsyncClient) -> None:
    expired = make_token(1, expire_hours=-1)
    response = await client.get("/api/user", headers=bearer(expired))
    assert_error(response, 401, "invalid or expired token")


async def test_token_for_missing_user_is_rejected(client: httpx.AsyncClient) -> None:
    ghost = make_token(999_999, expire_hours=1)
    response = await client.get("/api/user", headers=bearer(ghost))
    assert_error(response, 401, "user not found")


async def test_public_endpoints_do_not_require_token(client: httpx.AsyncClient) -> None:
    assert (await client.get("/health")).status_code == 200
    assert (await client.get("/api/config")).status_code == 200
    # 公开但凭据错误：返回业务 401，而不是「invalid or expired token」
    response = await client.post(
        "/api/auth/login", json={"email": "test@xianting.cn", "password": "bad"}
    )
    assert_error(response, 401, "邮箱或密码错误")
    response = await client.post("/api/auth/register", json={})
    assert_error(response, 400, "邮箱必填，密码至少6位")


async def test_options_preflight_is_allowed(client: httpx.AsyncClient) -> None:
    response = await client.options(
        "/api/user",
        headers={
            "Origin": "http://localhost:5173",
            "Access-Control-Request-Method": "GET",
            "Access-Control-Request-Headers": "authorization",
        },
    )
    assert response.status_code == 200
    assert response.headers["access-control-allow-origin"] == "*"


async def test_cross_user_conversation_is_invisible(client: httpx.AsyncClient) -> None:
    other = await register(client, "other@example.com")
    headers = bearer(other["token"])

    listed = assert_ok(await client.get("/api/convos", headers=headers))["data"]
    assert listed["conversations"] == []

    for method, path, body in [
        ("GET", f"/api/convos/{SEED_CONVERSATION_ID}/messages", None),
        ("PATCH", f"/api/convos/{SEED_CONVERSATION_ID}", {"title": "劫持"}),
        ("DELETE", f"/api/convos/{SEED_CONVERSATION_ID}", None),
    ]:
        response = await client.request(method, path, json=body, headers=headers)
        assert_error(response, 404, "conversation not found")

    # 种子用户的会话没有被其他用户破坏
    mine = assert_ok(await client.get("/api/convos", headers=make_seed_headers()))["data"]
    assert SEED_CONVERSATION_ID in [c["_id"] for c in mine["conversations"]]


async def test_cross_user_keyword_is_invisible(client: httpx.AsyncClient) -> None:
    other = await register(client, "other-keyword@example.com")
    headers = bearer(other["token"])

    response = await client.post(
        f"/api/inspiration/keywords/{SEED_KEYWORD_ID}/refresh", headers=headers
    )
    assert_error(response, 404, "keyword not found")

    response = await client.delete(f"/api/inspiration/keywords/{SEED_KEYWORD_ID}", headers=headers)
    assert_error(response, 404, "keyword not found")

    listed = assert_ok(await client.get("/api/inspiration", headers=headers))["data"]
    assert listed["data"]["keywords"] == []


def make_seed_headers() -> dict[str, str]:
    return bearer(issue_token(1, settings.app_jwt_secret, 1))
