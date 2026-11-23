"""用户仓储（对应 Java `UserRepository`）。"""

from sqlalchemy import select

from app.models.user import User
from app.repositories.base import BaseRepository


class UserRepository(BaseRepository[User]):
    model = User

    async def find_by_email(self, email: str) -> User | None:
        return await self.session.scalar(select(User).where(User.email == email))

    async def exists_by_email(self, email: str) -> bool:
        user_id = await self.session.scalar(select(User.id).where(User.email == email).limit(1))
        return user_id is not None
