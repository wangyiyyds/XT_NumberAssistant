"""灵感关键词 / 视频仓储（对应 Java 两个 Inspiration Repository）。"""

from sqlalchemy import delete, func, select

from app.models.inspiration import InspirationKeyword, InspirationVideo
from app.repositories.base import BaseRepository


class InspirationKeywordRepository(BaseRepository[InspirationKeyword]):
    model = InspirationKeyword

    async def list_by_user_id(self, user_id: int) -> list[InspirationKeyword]:
        """用户关键词列表，按 sort_order 升序。"""

        result = await self.session.scalars(
            select(InspirationKeyword)
            .where(InspirationKeyword.user_id == user_id)
            .order_by(InspirationKeyword.sort_order)
        )
        return list(result.all())

    async def count_by_user_id(self, user_id: int) -> int:
        result = await self.session.scalar(
            select(func.count())
            .select_from(InspirationKeyword)
            .where(InspirationKeyword.user_id == user_id)
        )
        return int(result or 0)

    async def exists_by_user_id_and_keyword(self, user_id: int, keyword: str) -> bool:
        keyword_id = await self.session.scalar(
            select(InspirationKeyword.id)
            .where(
                InspirationKeyword.user_id == user_id,
                InspirationKeyword.keyword == keyword,
            )
            .limit(1)
        )
        return keyword_id is not None

    async def find_by_id_and_user_id(self, id_: int, user_id: int) -> InspirationKeyword | None:
        return await self.session.scalar(
            select(InspirationKeyword).where(
                InspirationKeyword.id == id_,
                InspirationKeyword.user_id == user_id,
            )
        )


class InspirationVideoRepository(BaseRepository[InspirationVideo]):
    model = InspirationVideo

    async def list_by_keyword_id(self, keyword_id: int) -> list[InspirationVideo]:
        """关键词下视频，按 id 倒序。"""

        result = await self.session.scalars(
            select(InspirationVideo)
            .where(InspirationVideo.keyword_id == keyword_id)
            .order_by(InspirationVideo.id.desc())
        )
        return list(result.all())

    async def delete_by_keyword_id(self, keyword_id: int) -> None:
        async with self._transaction():
            await self.session.execute(
                delete(InspirationVideo).where(InspirationVideo.keyword_id == keyword_id)
            )
