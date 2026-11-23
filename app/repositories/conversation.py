"""会话仓储（对应 Java `ConversationRepository`）。"""

from sqlalchemy import select

from app.models.conversation import Conversation
from app.repositories.base import BaseRepository


class ConversationRepository(BaseRepository[Conversation]):
    model = Conversation

    async def list_by_user(self, user_id: int, agent_id: int | None = None) -> list[Conversation]:
        """用户会话列表，按 updated_at 倒序；agent_id 为 None 表示不过滤。"""

        stmt = select(Conversation).where(Conversation.user_id == user_id)
        if agent_id is not None:
            stmt = stmt.where(Conversation.agent_id == agent_id)
        stmt = stmt.order_by(Conversation.updated_at.desc())
        result = await self.session.scalars(stmt)
        return list(result.all())

    async def find_by_public_id_and_user_id(
        self, public_id: str, user_id: int
    ) -> Conversation | None:
        return await self.session.scalar(
            select(Conversation).where(
                Conversation.public_id == public_id,
                Conversation.user_id == user_id,
            )
        )
