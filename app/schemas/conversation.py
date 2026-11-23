"""ConversationDto（对应 Java `dto/ConversationDto`）及列表响应。"""

from pydantic import BaseModel, ConfigDict, Field

from app.models.conversation import Conversation
from app.schemas.types import JavaDateTime


class ConversationDto(BaseModel):
    """会话对外字段；`agent_id` 仅 SSE 事件使用，REST 列表恒为 null（序列化时省略）。"""

    model_config = ConfigDict(populate_by_name=True)

    id: str = Field(alias="_id")
    conversation_id: str = Field(alias="conversationId")
    title: str | None = None
    endpoint: str
    created_at: JavaDateTime = Field(alias="createdAt")
    updated_at: JavaDateTime = Field(alias="updatedAt")
    agent_id: str | None = Field(default=None, alias="agent_id")

    @classmethod
    def from_entity(cls, conversation: Conversation) -> "ConversationDto":
        return cls(
            id=conversation.public_id,
            conversation_id=conversation.public_id,
            title=conversation.title,
            endpoint=conversation.endpoint,
            created_at=conversation.created_at,
            updated_at=conversation.updated_at,
        )


class ConversationListResponse(BaseModel):
    """`GET /api/convos` 的 data 结构：{"conversations": [...]}。"""

    conversations: list[ConversationDto]


class RenameConversationRequest(BaseModel):
    title: str | None = None
