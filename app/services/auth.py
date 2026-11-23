"""认证服务（对应 Java `AuthServiceImpl`）。"""

import uuid
from datetime import datetime

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.exceptions import ApiException
from app.core.security import hash_password, issue_token, verify_password
from app.models.balance import Balance
from app.models.user import User
from app.repositories.balance import BalanceRepository
from app.repositories.user import UserRepository
from app.schemas.auth import AuthResponse
from app.schemas.user import UserDto


def _plus_years(value: datetime, years: int) -> datetime:
    """等价于 Java `LocalDateTime.plusYears`（2 月 29 日回退到 28 日）。"""

    try:
        return value.replace(year=value.year + years)
    except ValueError:
        return value.replace(year=value.year + years, day=28)


class AuthService:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session
        self.users = UserRepository(session)
        self.balances = BalanceRepository(session)

    async def login(self, email: str | None, password: str | None) -> AuthResponse:
        if email is None or password is None:
            raise ApiException(400, "email and password are required")

        user = await self.users.find_by_email(email.strip().lower())
        if user is None or not verify_password(password, user.password):
            raise ApiException(401, "邮箱或密码错误")
        if user.disabled:
            raise ApiException(403, "账号已禁用")
        return self._token_with_user(user)

    async def register(
        self,
        email: str | None,
        password: str | None,
        name: str | None,
        brand: str | None,
    ) -> AuthResponse:
        if email is None or password is None or len(password) < 6:
            raise ApiException(400, "邮箱必填，密码至少6位")

        normalized_email = email.strip().lower()
        now = datetime.now()

        # 对应 Java `@Transactional`：用户与余额同一事务提交
        async with self.session.begin():
            if await self.users.exists_by_email(normalized_email):
                raise ApiException(409, "该邮箱已注册")

            user = User(
                public_id=str(uuid.uuid4()),
                brand=brand if brand is not None else "xianting",
                email=normalized_email,
                password=hash_password(password),
                name=name if name is not None else "新用户",
                email_verified=False,
                disabled=False,
                created_at=now,
            )
            await self.users.save(user)

            balance = Balance(
                user_id=user.id,
                token_credits=settings.app_balance_start,
                auto_refill_enabled=True,
                expire_at=_plus_years(now, 1),
            )
            await self.balances.save(balance)

        return self._token_with_user(user)

    @staticmethod
    def refresh_token(user_id: int) -> str:
        return issue_token(user_id, settings.app_jwt_secret, settings.app_jwt_expire_hours)

    def _token_with_user(self, user: User) -> AuthResponse:
        token = issue_token(user.id, settings.app_jwt_secret, settings.app_jwt_expire_hours)
        return AuthResponse(token=token, user=UserDto.from_entity(user))
