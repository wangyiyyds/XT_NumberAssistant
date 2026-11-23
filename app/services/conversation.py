"""会话服务（对应 Java `ConversationServiceImpl`）。"""

from __future__ import annotations

from datetime import datetime

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ApiException
from app.db.session import atomic
from app.models.conversation import Conversation
from app.repositories.conversation import ConversationRepository
from app.repositories.message import MessageRepository
from app.schemas.common import SuccessResponse
from app.schemas.conversation import ConversationDto, ConversationListResponse
from app.schemas.message import MessageDto
from app.services.agent import AgentService


class ConversationService:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session
        self.conversation_repo = ConversationRepository(session)
        self.message_repo = MessageRepository(session)
        self.agent_service = AgentService(session)

    async def list(self, user_id: int, agent_public_id: str | None) -> ConversationListResponse:
        agent_id: int | None = None
        if agent_public_id is not None and agent_public_id.strip():
            agent_id = (await self.agent_service.require_by_public_id(agent_public_id)).id

        rows = await self.conversation_repo.list_by_user(user_id, agent_id)
        return ConversationListResponse(
            conversations=[ConversationDto.from_entity(row) for row in rows]
        )

    async def messages(self, user_id: int, public_id: str) -> list[MessageDto]:
        conversation = await self._require_own(public_id, user_id)
        rows = await self.message_repo.list_by_conversation_id(conversation.id)
        return [MessageDto.from_entity(row, conversation.public_id) for row in rows]

    async def rename(self, user_id: int, public_id: str, title: str | None) -> ConversationDto:
        conversation = await self._require_own(public_id, user_id)
        if title is None or not title.strip():
            raise ApiException(400, "title is required")

        conversation.title = title.strip()
        conversation.updated_at = datetime.now()
        await self.conversation_repo.update(
            conversation,
            title=conversation.title,
            updated_at=conversation.updated_at,
        )
        return ConversationDto.from_entity(conversation)

    async def delete(self, user_id: int, public_id: str) -> SuccessResponse:
        # 会话与消息同一事务删除
        async with atomic(self.session):
            conversation = await self._require_own(public_id, user_id)
            await self.message_repo.delete_by_conversation_id(conversation.id)
            await self.conversation_repo.remove(conversation)
        return SuccessResponse()

    async def _require_own(self, public_id: str, user_id: int) -> Conversation:
        # 越权访问与不存在统一返回 404，不暴露会话归属
        conversation = await self.conversation_repo.find_by_public_id_and_user_id(
            public_id, user_id
        )
        if conversation is None:
            raise ApiException(404, "conversation not found")
        return conversation
