"""t_agent 模型。"""

from datetime import datetime

from sqlalchemy import BigInteger, Boolean, DateTime, Identity, String, text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class Agent(Base):
    __tablename__ = "t_agent"

    id: Mapped[int] = mapped_column(BigInteger, Identity(start=23), primary_key=True)
    public_id: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    department_id: Mapped[int | None] = mapped_column(BigInteger)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str | None] = mapped_column(String(1024))
    avatar: Mapped[str | None] = mapped_column(String(512))
    category: Mapped[str | None] = mapped_column(String(64))
    is_preset: Mapped[bool] = mapped_column(Boolean, nullable=False, server_default=text("true"))
    # JSON 数组字符串，例如 ["starter1","starter2"]；Java 版按字符串存储
    conversation_starters: Mapped[str | None] = mapped_column(String(2048))
    instructions: Mapped[str | None] = mapped_column(String)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=False), nullable=False, server_default=text("CURRENT_TIMESTAMP")
    )
