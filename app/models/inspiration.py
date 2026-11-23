"""t_inspiration_keyword / t_inspiration_video 模型。"""

from datetime import datetime

from sqlalchemy import BigInteger, Boolean, DateTime, Identity, Index, Integer, String, text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class InspirationKeyword(Base):
    __tablename__ = "t_inspiration_keyword"

    id: Mapped[int] = mapped_column(BigInteger, Identity(start=2), primary_key=True)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    keyword: Mapped[str] = mapped_column(String(128), nullable=False)
    sort_order: Mapped[int] = mapped_column(Integer, nullable=False, server_default=text("0"))
    cache_expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=False))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=False), nullable=False, server_default=text("CURRENT_TIMESTAMP")
    )


class InspirationVideo(Base):
    __tablename__ = "t_inspiration_video"
    __table_args__ = (Index("idx_video_keyword", "keyword_id"),)

    id: Mapped[int] = mapped_column(BigInteger, Identity(start=11), primary_key=True)
    keyword_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    author: Mapped[str | None] = mapped_column(String(128))
    fans: Mapped[str | None] = mapped_column(String(32))
    duration: Mapped[str | None] = mapped_column(String(16))
    publish_date: Mapped[str | None] = mapped_column(String(16))
    title: Mapped[str | None] = mapped_column(String(1024))
    likes: Mapped[str | None] = mapped_column(String(32))
    cover_url: Mapped[str | None] = mapped_column(String(512))
    video_url: Mapped[str | None] = mapped_column(String(512))
    low_fans_high_likes: Mapped[bool] = mapped_column(
        Boolean, nullable=False, server_default=text("false")
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=False), nullable=False, server_default=text("CURRENT_TIMESTAMP")
    )
