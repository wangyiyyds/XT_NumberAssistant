"""消息仓储（对应 Java `MessageRepository`）。"""

from sqlalchemy import delete, select

from app.models.message import Message
from app.repositories.base import BaseRepository


class MessageRepository(BaseRepository[Message]):
    model = Message

    async def list_by_conversation_id(self, conversation_id: int) -> list[Message]:
        """会话内消息，按 id 升序。"""

        result = await self.session.scalars(
            select(Message).where(Message.conversation_id == conversation_id).order_by(Message.id)
        )
        return list(result.all())

    async def delete_by_conversation_id(self, conversation_id: int) -> None:
        async with self._transaction():
            await self.session.execute(
                delete(Message).where(Message.conversation_id == conversation_id)
            )
