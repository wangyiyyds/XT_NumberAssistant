"""余额服务（对应 Java `BalanceServiceImpl`）。"""

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ApiException
from app.models.user import User
from app.repositories.balance import BalanceRepository
from app.schemas.balance import BalanceDto


class BalanceService:
    def __init__(self, session: AsyncSession) -> None:
        self.balances = BalanceRepository(session)

    async def get_balance(self, user: User) -> BalanceDto:
        balance = await self.balances.find_by_user_id(user.id)
        return BalanceDto.from_entity(balance, user.public_id)

    async def deduct(self, user_id: int, cost: int) -> None:
        """静默扣减（余额不足扣到 0），用于按条计费的对话。"""

        balance = await self.balances.find_by_user_id(user_id)
        if balance is None:
            return
        credits = balance.token_credits if balance.token_credits is not None else 0
        await self.balances.update(balance, token_credits=max(0, credits - cost))

    async def deduct_or_throw(self, user_id: int, cost: int) -> None:
        """强制扣减积分；余额不足抛 402（用于付费刷新等操作）。"""

        balance = await self.balances.find_by_user_id(user_id)
        credits = balance.token_credits if balance is not None else 0
        if credits < cost:
            raise ApiException(402, f"积分不足，刷新需要{cost}积分")
        await self.balances.update(balance, token_credits=credits - cost)
