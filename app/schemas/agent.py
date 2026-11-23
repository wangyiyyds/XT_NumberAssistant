"""AgentDto（对应 Java `dto/AgentDto`）及 my-access 响应。"""

import json

from pydantic import BaseModel, ConfigDict, Field

from app.models.agent import Agent
from app.schemas.types import JavaDateTime


def parse_conversation_starters(raw: str | None) -> list[str]:
    """解析数据库中的 JSON 数组字符串；无法解析时返回空数组。

    与 Java `AgentConverter.toStarters` 一致：逐项去空白、丢弃空串。
    """

    if raw is None or not raw.strip():
        return []
    try:
        parsed = json.loads(raw)
    except (TypeError, ValueError):
        return []
    if not isinstance(parsed, list):
        return []
    return [item.strip() for item in parsed if isinstance(item, str) and item.strip()]


class AgentDto(BaseModel):
    """智能体对外字段；`_id` 与 `agent_id` 同为 publicId。"""

    model_config = ConfigDict(populate_by_name=True)

    id: str = Field(alias="_id")
    agent_id: str = Field(alias="agent_id")
    name: str
    description: str | None = None
    avatar: str | None = None
    category: str | None = None
    department: str | None = None
    is_preset: bool = Field(alias="isPreset")
    conversation_starters: list[str] = Field(default_factory=list, alias="conversation_starters")
    instructions: str | None = None
    created_at: JavaDateTime = Field(alias="createdAt")

    @classmethod
    def from_entity(cls, agent: Agent, department_name: str | None) -> "AgentDto":
        return cls(
            id=agent.public_id,
            agent_id=agent.public_id,
            name=agent.name,
            description=agent.description,
            avatar=agent.avatar,
            category=agent.category,
            department=department_name,
            is_preset=agent.is_preset,
            conversation_starters=parse_conversation_starters(agent.conversation_starters),
            instructions=agent.instructions,
            created_at=agent.created_at,
        )


class AgentAccessResponse(BaseModel):
    """`GET /api/agents/my-access` 的 data 结构：{"data": [publicId, ...]}。"""

    data: list[str]
