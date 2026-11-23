"""t_user 模型。"""

from datetime import datetime

from sqlalchemy import BigInteger, Boolean, DateTime, Identity, String, text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class User(Base):
    __tablename__ = "t_user"

    id: Mapped[int] = mapped_column(BigInteger, Identity(start=2), primary_key=True)
    public_id: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    brand: Mapped[str] = mapped_column(
        String(32), nullable=False, server_default=text("'xianting'")
    )
    email: Mapped[str] = mapped_column(String(128), nullable=False, unique=True)
    password: Mapped[str] = mapped_column(String(256), nullable=False)
    name: Mapped[str | None] = mapped_column(String(64))
    avatar: Mapped[str | None] = mapped_column(String(256))
    email_verified: Mapped[bool] = mapped_column(
        Boolean, nullable=False, server_default=text("false")
    )
    disabled: Mapped[bool] = mapped_column(Boolean, nullable=False, server_default=text("false"))
    contract_read_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=False))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=False), nullable=False, server_default=text("CURRENT_TIMESTAMP")
    )
