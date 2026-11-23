"""单元测试：DTO 转换与 Jackson `non_null` 等价的序列化结果。"""

from __future__ import annotations

from datetime import datetime

from app.models.agent import Agent
from app.models.balance import Balance
from app.models.conversation import Conversation
from app.models.department import Department
from app.models.inspiration import InspirationKeyword, InspirationVideo
from app.models.message import Message
from app.models.user import User
from app.schemas.agent import AgentDto, parse_conversation_starters
from app.schemas.balance import BalanceDto
from app.schemas.conversation import ConversationDto
from app.schemas.department import DepartmentDto
from app.schemas.inspiration import InspirationKeywordDto, InspirationVideoDto
from app.schemas.message import MessageDto
from app.schemas.user import UserDto

NOW = datetime(2026, 11, 23, 14, 51, 19, 123000)


def dump(dto: object) -> dict:
    return dto.model_dump(mode="json", by_alias=True, exclude_none=True)  # type: ignore[attr-defined]


def test_user_dto_aliases_and_null_omission() -> None:
    user = User(
        public_id="u-public",
        brand="xianting",
        email="a@b.c",
        name=None,
        avatar=None,
        email_verified=True,
        disabled=False,
        contract_read_at=None,
        created_at=NOW,
    )
    payload = dump(UserDto.from_entity(user))
    assert payload == {
        "_id": "u-public",
        "brand": "xianting",
        "email": "a@b.c",
        "emailVerified": True,
        "disabled": False,
        "createdAt": "2026-11-23T14:51:19.123",
    }
    assert "password" not in payload


def test_agent_dto_aliases_and_starters() -> None:
    agent = Agent(
        public_id="agent-public",
        department_id=1,
        name="爆款选题助手",
        description="描述",
        avatar=None,
        category="流量部门",
        is_preset=True,
        conversation_starters='["你好","帮我分析"]',
        instructions="人设",
        created_at=NOW,
    )
    payload = dump(AgentDto.from_entity(agent, "流量部门"))
    assert payload["_id"] == "agent-public"
    assert payload["agent_id"] == "agent-public"
    assert payload["department"] == "流量部门"
    assert payload["isPreset"] is True
    assert payload["conversation_starters"] == ["你好", "帮我分析"]
    assert "avatar" not in payload
    assert payload["createdAt"] == "2026-11-23T14:51:19.123"


def test_parse_conversation_starters_rules() -> None:
    assert parse_conversation_starters(None) == []
    assert parse_conversation_starters("") == []
    assert parse_conversation_starters("   ") == []
    assert parse_conversation_starters("not-json") == []
    assert parse_conversation_starters('{"a":1}') == []
    assert parse_conversation_starters('[" a ","", 3, "b"]') == ["a", "b"]


def test_department_dto_extra_tools_omitted_and_version_zero() -> None:
    department = Department(
        public_id="dept-public",
        name="流量部门",
        enabled=True,
        brand_only=False,
        sort_order=0,
    )
    payload = dump(DepartmentDto.from_entity(department))
    assert payload == {
        "_id": "dept-public",
        "name": "流量部门",
        "enabled": True,
        "brandOnly": False,
        "sortOrder": 0,
        "__v": 0,
    }
    assert "extraTools" not in payload


def test_conversation_dto_hides_agent_id_by_default() -> None:
    conversation = Conversation(
        public_id="conv-public",
        user_id=1,
        agent_id=1,
        title="标题",
        endpoint="agents",
        created_at=NOW,
        updated_at=NOW,
    )
    dto = ConversationDto.from_entity(conversation)
    payload = dump(dto)
    assert payload["_id"] == "conv-public"
    assert payload["conversationId"] == "conv-public"
    assert payload["createdAt"] == payload["updatedAt"] == "2026-11-23T14:51:19.123"
    assert "agent_id" not in payload

    dto.agent_id = "agent-public"
    assert dump(dto)["agent_id"] == "agent-public"


def test_message_dto_is_created_by_user() -> None:
    user_message = Message(
        public_id="m1",
        conversation_id=1,
        parent_message_id=None,
        sender="user",
        text="你好",
        error=False,
        created_at=NOW,
    )
    agent_message = Message(
        public_id="m2",
        conversation_id=1,
        parent_message_id="m1",
        sender="agent",
        text="回复",
        error=False,
        created_at=NOW,
    )
    user_payload = dump(MessageDto.from_entity(user_message, "conv-public"))
    agent_payload = dump(MessageDto.from_entity(agent_message, "conv-public"))

    assert user_payload["isCreatedByUser"] is True
    assert "parentMessageId" not in user_payload
    assert agent_payload["isCreatedByUser"] is False
    assert agent_payload["parentMessageId"] == "m1"
    assert agent_payload["conversationId"] == "conv-public"
    assert agent_payload["messageId"] == "m2"


def test_balance_dto_falls_back_when_missing() -> None:
    payload = dump(BalanceDto.from_entity(None, "u-public"))
    assert payload == {
        "tokenCredits": 0,
        "autoRefillEnabled": False,
        "user": "u-public",
    }
    assert "expireAt" not in payload


def test_balance_dto_from_entity() -> None:
    balance = Balance(
        user_id=1,
        token_credits=99980,
        auto_refill_enabled=True,
        expire_at=NOW,
    )
    payload = dump(BalanceDto.from_entity(balance, "u-public"))
    assert payload["tokenCredits"] == 99980
    assert payload["autoRefillEnabled"] is True
    assert payload["expireAt"] == "2026-11-23T14:51:19.123"
    assert payload["user"] == "u-public"


def test_inspiration_dtos_use_database_ids() -> None:
    keyword = InspirationKeyword(
        id=1,
        user_id=1,
        keyword="Aigc培训",
        sort_order=0,
        cache_expires_at=None,
        created_at=NOW,
    )
    video = InspirationVideo(
        id=11,
        keyword_id=1,
        author="作者",
        fans="4.1w",
        duration="28:40",
        publish_date="2026-05-03",
        title="标题",
        likes="23.3w",
        cover_url=None,
        video_url=None,
        low_fans_high_likes=True,
        created_at=NOW,
    )
    payload = dump(InspirationKeywordDto.from_entity(keyword, [video]))
    assert payload["_id"] == 1
    assert payload["order"] == 0
    assert "cacheExpiresAt" not in payload
    assert payload["videos"][0] == {
        "_id": 11,
        "author": "作者",
        "fans": "4.1w",
        "duration": "28:40",
        "publishDate": "2026-05-03",
        "title": "标题",
        "likes": "23.3w",
        "lowFansHighLikes": True,
    }
    assert dump(InspirationVideoDto.from_entity(video))["publishDate"] == "2026-05-03"
