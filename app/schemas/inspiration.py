"""灵感中心 DTO（对应 Java `dto/Inspiration*Dto`）。"""

from pydantic import BaseModel, ConfigDict, Field

from app.models.inspiration import InspirationKeyword, InspirationVideo
from app.schemas.types import JavaDateTime


class InspirationVideoDto(BaseModel):
    """视频对外字段；`_id` 直接使用数据库自增 id。"""

    model_config = ConfigDict(populate_by_name=True)

    id: int = Field(alias="_id")
    author: str | None = None
    fans: str | None = None
    duration: str | None = None
    publish_date: str | None = Field(default=None, alias="publishDate")
    title: str | None = None
    likes: str | None = None
    cover_url: str | None = Field(default=None, alias="coverUrl")
    video_url: str | None = Field(default=None, alias="videoUrl")
    low_fans_high_likes: bool = Field(alias="lowFansHighLikes")

    @classmethod
    def from_entity(cls, video: InspirationVideo) -> "InspirationVideoDto":
        return cls(
            id=video.id,
            author=video.author,
            fans=video.fans,
            duration=video.duration,
            publish_date=video.publish_date,
            title=video.title,
            likes=video.likes,
            cover_url=video.cover_url,
            video_url=video.video_url,
            low_fans_high_likes=bool(video.low_fans_high_likes),
        )


class InspirationKeywordDto(BaseModel):
    """关键词对外字段；`_id` 直接使用数据库自增 id，`order` 对应 sortOrder。"""

    model_config = ConfigDict(populate_by_name=True)

    id: int = Field(alias="_id")
    keyword: str
    order: int
    cache_expires_at: JavaDateTime | None = Field(default=None, alias="cacheExpiresAt")
    videos: list[InspirationVideoDto] = Field(default_factory=list)

    @classmethod
    def from_entity(
        cls, keyword: InspirationKeyword, videos: list[InspirationVideo]
    ) -> "InspirationKeywordDto":
        return cls(
            id=keyword.id,
            keyword=keyword.keyword,
            order=keyword.sort_order,
            cache_expires_at=keyword.cache_expires_at,
            videos=[InspirationVideoDto.from_entity(video) for video in videos],
        )


class InspirationKeywordsPayload(BaseModel):
    keywords: list[InspirationKeywordDto]


class InspirationListResponse(BaseModel):
    """`GET /api/inspiration` 的 data 结构：{"data": {"keywords": [...]}}。"""

    data: InspirationKeywordsPayload


class AddKeywordRequest(BaseModel):
    keyword: str | None = None
