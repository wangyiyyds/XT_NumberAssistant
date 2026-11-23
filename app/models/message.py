"""t_message 模型。"""

from datetime import datetime

from sqlalchemy import BigInteger, Boolean, DateTime, Identity, Index, String, Text
from sqlalchemy import text as sql_text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class Message(Base):
    __tablename__ = "t_message"
    __table_args__ = (Index("idx_message_conversation", "conversation_id"),)

    id: Mapped[int] = mapped_column(BigInteger, Identity(start=5), primary_key=True)
    public_id: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    conversation_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    parent_message_id: Mapped[str | None] = mapped_column(String(64))
    # "user" 或 "agent"
    sender: Mapped[str] = mapped_column(String(16), nullable=False)
    text: Mapped[str | None] = mapped_column(Text)
    error: Mapped[bool] = mapped_column(Boolean, nullable=False, server_default=sql_text("false"))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=False), nullable=False, server_default=sql_text("CURRENT_TIMESTAMP")
    )
