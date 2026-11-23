"""余额仓储（对应 Java `BalanceRepository`）。"""

from sqlalchemy import select

from app.models.balance import Balance
from app.repositories.base import BaseRepository


class BalanceRepository(BaseRepository[Balance]):
    model = Balance

    async def find_by_user_id(self, user_id: int) -> Balance | None:
        return await self.session.scalar(select(Balance).where(Balance.user_id == user_id))
