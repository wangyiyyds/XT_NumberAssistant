"""t_prompt 模型（仅建表与数据，无接口）。

- `active_agent_id` 是数据库生成列，应用层不写入。
- `updated_at` 由 `db/schema.sql` 中的触发器维护，应用层不重复处理。
"""

from datetime import datetime
from typing import Any

from sqlalchemy import (
    BigInteger,
    Computed,
    DateTime,
    ForeignKey,
    Identity,
    Index,
    Integer,
    String,
    Text,
    UniqueConstraint,
    text,
)
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class Prompt(Base):
    __tablename__ = "t_prompt"
    __table_args__ = (
        UniqueConstraint("public_id", name="uk_public_id"),
        UniqueConstraint("agent_id", "version", name="uk_agent_version"),
        UniqueConstraint("active_agent_id", name="uk_agent_active"),
        Index("idx_agent_status", "agent_id", "status"),
        Index("idx_agent_created", "agent_id", "created_at"),
        Index("idx_parent", "parent_prompt_id"),
    )

    id: Mapped[int] = mapped_column(BigInteger, Identity(start=1030), primary_key=True)
    public_id: Mapped[str] = mapped_column(String(64), nullable=False)
    agent_id: Mapped[int] = mapped_column(
        BigInteger,
        ForeignKey("t_agent.id", ondelete="CASCADE", onupdate="RESTRICT"),
        nullable=False,
    )
    version: Mapped[int] = mapped_column(Integer, nullable=False)
    version_tag: Mapped[str | None] = mapped_column(String(32))
    name: Mapped[str | None] = mapped_column(String(128))
    description: Mapped[str | None] = mapped_column(String(1024))
    prompt_type: Mapped[str] = mapped_column(
        String(32), nullable=False, server_default=text("'SYSTEM'")
    )
    content: Mapped[str] = mapped_column(Text, nullable=False)
    variables: Mapped[dict[str, Any] | None] = mapped_column(JSONB)
    model_config: Mapped[dict[str, Any] | None] = mapped_column(JSONB)
    status: Mapped[str] = mapped_column(String(16), nullable=False, server_default=text("'DRAFT'"))
    parent_prompt_id: Mapped[int | None] = mapped_column(
        BigInteger,
        ForeignKey("t_prompt.id", ondelete="SET NULL", onupdate="RESTRICT"),
    )
    created_by: Mapped[str | None] = mapped_column(String(64))
    updated_by: Mapped[str | None] = mapped_column(String(64))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=False), nullable=False, server_default=text("CURRENT_TIMESTAMP")
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=False), nullable=False, server_default=text("CURRENT_TIMESTAMP")
    )
    published_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=False))
    archived_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=False))
    active_agent_id: Mapped[int | None] = mapped_column(
        BigInteger,
        Computed("CASE WHEN status = 'ACTIVE' THEN agent_id END", persisted=True),
        nullable=True,
    )
