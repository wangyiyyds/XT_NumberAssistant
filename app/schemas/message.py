"""MessageDto（对应 Java `dto/MessageDto`）。"""

from pydantic import BaseModel, ConfigDict, Field

from app.models.message import Message
from app.schemas.types import JavaDateTime


class MessageDto(BaseModel):
    """消息对外字段；`isCreatedByUser` 由 sender 计算，`conversationId` 用会话 publicId。"""

    model_config = ConfigDict(populate_by_name=True)

    id: str = Field(alias="_id")
    message_id: str = Field(alias="messageId")
    conversation_id: str = Field(alias="conversationId")
    parent_message_id: str | None = Field(default=None, alias="parentMessageId")
    sender: str
    is_created_by_user: bool = Field(alias="isCreatedByUser")
    text: str | None = None
    error: bool
    created_at: JavaDateTime = Field(alias="createdAt")

    @classmethod
    def from_entity(cls, message: Message, conversation_public_id: str) -> "MessageDto":
        return cls(
            id=message.public_id,
            message_id=message.public_id,
            conversation_id=conversation_public_id,
            parent_message_id=message.parent_message_id,
            sender=message.sender,
            is_created_by_user=message.sender == "user",
            text=message.text,
            error=bool(message.error),
            created_at=message.created_at,
        )
