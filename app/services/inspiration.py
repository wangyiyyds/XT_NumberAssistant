"""灵感中心服务（对应 Java `InspirationServiceImpl`）。"""

from datetime import datetime, timedelta

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ApiException
from app.db.session import atomic
from app.models.inspiration import InspirationKeyword
from app.repositories.inspiration import (
    InspirationKeywordRepository,
    InspirationVideoRepository,
)
from app.schemas.common import SuccessResponse
from app.schemas.inspiration import (
    InspirationKeywordDto,
    InspirationKeywordsPayload,
    InspirationListResponse,
)
from app.services.balance import BalanceService

MAX_KEYWORDS = 5
REFRESH_COST = 60


class InspirationService:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session
        self.keywords = InspirationKeywordRepository(session)
        self.videos = InspirationVideoRepository(session)
        self.balances = BalanceService(session)

    async def list(self, user_id: int) -> InspirationListResponse:
        keywords = []
        for keyword in await self.keywords.list_by_user_id(user_id):
            videos = await self.videos.list_by_keyword_id(keyword.id)
            keywords.append(InspirationKeywordDto.from_entity(keyword, videos))
        return InspirationListResponse(data=InspirationKeywordsPayload(keywords=keywords))

    async def add_keyword(self, user_id: int, keyword: str | None) -> InspirationKeywordDto:
        if keyword is None or not keyword.strip():
            raise ApiException(400, "keyword is required")
        keyword = keyword.strip()

        count = await self.keywords.count_by_user_id(user_id)
        if count >= MAX_KEYWORDS:
            raise ApiException(400, f"最多{MAX_KEYWORDS}个关键词")
        if await self.keywords.exists_by_user_id_and_keyword(user_id, keyword):
            raise ApiException(409, "关键词已存在")

        now = datetime.now()
        entity = InspirationKeyword(
            user_id=user_id,
            keyword=keyword,
            sort_order=count,
            cache_expires_at=now + timedelta(hours=24),
            created_at=now,
        )
        await self.keywords.save(entity)
        return InspirationKeywordDto.from_entity(entity, [])

    async def delete_keyword(self, user_id: int, id_: int) -> SuccessResponse:
        # 关键词与视频同一事务删除
        async with atomic(self.session):
            keyword = await self._require_own(id_, user_id)
            await self.videos.delete_by_keyword_id(keyword.id)
            await self.keywords.remove(keyword)
        return SuccessResponse()

    async def refresh(self, user_id: int, id_: int) -> InspirationKeywordDto:
        """手动刷新：扣积分并续缓存窗口（视频来自种子数据池）。"""

        # 扣积分与续期必须原子完成，避免扣费成功但续期失败
        async with atomic(self.session):
            keyword = await self._require_own(id_, user_id)
            await self.balances.deduct_or_throw(user_id, REFRESH_COST)
            keyword.cache_expires_at = datetime.now() + timedelta(hours=24)
            await self.keywords.update(keyword, cache_expires_at=keyword.cache_expires_at)
            videos = await self.videos.list_by_keyword_id(keyword.id)
        return InspirationKeywordDto.from_entity(keyword, videos)

    async def _require_own(self, id_: int, user_id: int) -> InspirationKeyword:
        keyword = await self.keywords.find_by_id_and_user_id(id_, user_id)
        if keyword is None:
            raise ApiException(404, "keyword not found")
        return keyword
