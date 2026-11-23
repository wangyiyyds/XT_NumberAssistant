"""t_balance 模型。"""

from datetime import datetime

from sqlalchemy import BigInteger, Boolean, DateTime, Identity, text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class Balance(Base):
    __tablename__ = "t_balance"

    id: Mapped[int] = mapped_column(BigInteger, Identity(start=2), primary_key=True)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False, unique=True)
    token_credits: Mapped[int] = mapped_column(BigInteger, nullable=False, server_default=text("0"))
    auto_refill_enabled: Mapped[bool] = mapped_column(
        Boolean, nullable=False, server_default=text("false")
    )
    expire_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=False))
