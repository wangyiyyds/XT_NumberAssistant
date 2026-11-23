"""pytest 夹具与测试辅助。

关键约定：

- 测试库固定为 ``xianting_chat_test``（可用 ``TEST_DATABASE_URL`` 覆盖），
  绝不允许指向生产库；夹具创建库后执行 ``db/schema.sql`` + ``db/data.sql``，
  与生产结构 / 种子数据完全一致。
- 环境变量必须在 import ``app.*`` 之前写入，因为 ``app.core.config.settings``
  在 import 时缓存，``app.db.session.engine`` 也在 import 时创建。
- 每个用例开始前重置测试库，保证状态可重复；用例之间互不影响。
"""

from __future__ import annotations

import json
import os
import re
from collections.abc import AsyncIterator
from typing import Any

TEST_DATABASE_URL = (
    os.environ.get("TEST_DATABASE_URL")
    or "postgresql+asyncpg://xianting:xianting@localhost:5433/xianting_chat_test"
)

# ---- 必须在导入 app 之前设置 ----
from sqlalchemy.engine import make_url

_test_database = make_url(TEST_DATABASE_URL).database or ""
if not _test_database.endswith("_test"):
    raise RuntimeError(
        f"测试库名必须以 _test 结尾，当前为 {_test_database!r}；"
        "为避免误删生产数据，pytest 拒绝启动。"
    )

os.environ["DATABASE_URL"] = TEST_DATABASE_URL
os.environ["APP_ENV"] = "test"
os.environ["LOG_LEVEL"] = "WARNING"
# 测试中的 AI 调用一律 mock；固定一个非空值，只有显式测试缺失分支时才清空
os.environ["DEEPSEEK_API_KEY"] = "test-key"

import httpx
import pytest
import pytest_asyncio

from app.core.config import settings
from app.core.security import issue_token
from app.db.init import ensure_database, init_database
from app.db.session import SessionLocal
from app.main import app

TIMESTAMP_RE = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{3,6})?$")
TIME_RE = re.compile(r"\d{2}:\d{2}:\d{2}")


# ---------------------------------------------------------------------------
# 数据库
# ---------------------------------------------------------------------------


@pytest_asyncio.fixture(scope="session")
async def test_database() -> AsyncIterator[str]:
    """确保测试库存在（只在整轮测试开始时执行一次）。"""

    await ensure_database(TEST_DATABASE_URL)
    yield TEST_DATABASE_URL


@pytest_asyncio.fixture
async def reset_database(test_database: str) -> AsyncIterator[None]:
    """执行 schema.sql + data.sql，把测试库还原成初始状态。"""

    await init_database(test_database)
    yield


@pytest_asyncio.fixture
async def db_session(reset_database: None) -> AsyncIterator[Any]:
    """测试库会话；用完即关，不依赖 FastAPI 依赖注入。"""

    async with SessionLocal() as session:
        yield session


@pytest_asyncio.fixture
async def client(reset_database: None) -> AsyncIterator[httpx.AsyncClient]:
    """进程内 ASGI 客户端；不走真实网络。"""

    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as http:
        yield http


# ---------------------------------------------------------------------------
# 认证
# ---------------------------------------------------------------------------


def make_token(user_id: int = 1, expire_hours: int = 1) -> str:
    return issue_token(user_id, settings.app_jwt_secret, expire_hours)


def bearer(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture
def seed_token() -> str:
    """种子用户（test@xianting.cn，id=1）的 token。"""

    return make_token(1)


@pytest.fixture
def seed_headers(seed_token: str) -> dict[str, str]:
    return bearer(seed_token)


async def login(client: httpx.AsyncClient, email: str, password: str) -> dict[str, Any]:
    response = await client.post("/api/auth/login", json={"email": email, "password": password})
    assert response.status_code == 200, response.text
    return response.json()["data"]


async def register(
    client: httpx.AsyncClient, email: str, password: str = "123456", name: str = "测试用户B"
) -> dict[str, Any]:
    response = await client.post(
        "/api/auth/register",
        json={"email": email, "password": password, "name": name},
    )
    assert response.status_code == 200, response.text
    return response.json()["data"]


# ---------------------------------------------------------------------------
# 响应外壳 / SSE 解析
# ---------------------------------------------------------------------------


def assert_ok(response: httpx.Response, status: int = 200) -> dict[str, Any]:
    """断言成功外壳（Java Result 逐字段：code / message / timestamp，data 可选）。"""

    assert response.status_code == status, response.text
    body = response.json()
    assert body["code"] == 200
    assert body["message"] == "操作成功"
    assert set(body) <= {"code", "message", "data", "timestamp"}
    assert TIMESTAMP_RE.match(body["timestamp"]), body["timestamp"]
    return body


def assert_error(
    response: httpx.Response, status: int, message: str | None = None
) -> dict[str, Any]:
    """断言失败外壳：HTTP 状态码与 code 一致，data 不序列化。"""

    assert response.status_code == status, response.text
    body = response.json()
    assert body["code"] == status
    assert body["message"]
    assert "data" not in body
    assert TIMESTAMP_RE.match(body["timestamp"]), body["timestamp"]
    if message is not None:
        assert body["message"] == message
    return body


def parse_sse(body: str) -> list[tuple[str, dict[str, Any]]]:
    """把 ``event:xxx\\ndata:{json}\\n\\n`` 解析成 (事件名, data) 列表。"""

    frames: list[tuple[str, dict[str, Any]]] = []
    for block in body.split("\n\n"):
        block = block.strip("\r\n")
        if not block:
            continue
        event = None
        data_lines: list[str] = []
        for line in block.split("\n"):
            if line.startswith("event:"):
                event = line[len("event:") :]
            elif line.startswith("data:"):
                data_lines.append(line[len("data:") :])
        assert event is not None, block
        assert data_lines, block
        frames.append((event, json.loads("\n".join(data_lines))))
    return frames


async def read_sse(response: httpx.Response) -> list[tuple[str, dict[str, Any]]]:
    body = "".join([chunk async for chunk in response.aiter_text()])
    return parse_sse(body)


# ---------------------------------------------------------------------------
# 种子数据常量（与 db/data.sql 一致）
# ---------------------------------------------------------------------------

SEED_USER_ID = 1
SEED_USER_PUBLIC_ID = "f06b74c4-40cd-46fb-b44f-359a2a587565"
SEED_USER_EMAIL = "test@xianting.cn"
SEED_USER_PASSWORD = "123456"
SEED_BALANCE = 99980

SEED_CONVERSATION_ID = "6650d5ec-10ad-4b80-ba58-d8ae2ea0a7d4"
SEED_CONVERSATION_ID_2 = "a8bdd291-91d7-4fca-be3a-f133b65a84f9"
SEED_DEPARTMENT_COUNT = 7
SEED_AGENT_COUNT = 22
SEED_KEYWORD_ID = 1
SEED_KEYWORD_TEXT = "Aigc培训"
SEED_VIDEO_COUNT = 10

AGENT_PUBLIC_ID = "8b9b54dd-0015-4d4f-804f-1e001e6663b5"  # 爆款选题助手
AGENT_NAME = "爆款选题助手"
