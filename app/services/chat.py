"""SSE 流式对话编排（对应 Java `ChatServiceImpl`）。

执行顺序照搬 Java 版：

1. 校验 `agent_id` / `text`（缺失 400），按 publicId 查智能体（不存在 404）——
   这两步在路由层、开始推流之前完成，失败返回统一 JSON 外壳。
2. 有 `conversationId` 则按「publicId + userId」查归属，查不到就新建会话。
3. 新会话先发 `conversation` 事件。
4. 扣 10 积分（静默扣减，余额不足照常继续）。
5. 落库用户消息（`messageId` 可由前端传入）。
6. 查会话全部消息，按 id 升序，排除本次用户消息，取最近 10 条作上下文。
7. 调 DeepSeek 流式接口，逐段发 `message`。
8. 落库助手消息、更新会话 `updated_at`，发 `final`。
9. 任意异常发 `error`；已扣积分不退还。
10. 客户端断开：停止消费上游流、释放连接，不再写库。

事务：流式开始前、结束后各用短事务（独立 session，用完即关），
`yield` 期间不持有 session，也不持有数据库事务。
"""

from __future__ import annotations

import asyncio
import json
import logging
import uuid
from collections.abc import AsyncIterator
from dataclasses import dataclass
from datetime import datetime
from typing import Any

from fastapi import Request
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.core.exceptions import ApiException
from app.db.session import SessionLocal
from app.models.agent import Agent
from app.models.conversation import Conversation
from app.models.message import Message
from app.repositories.agent import AgentRepository
from app.repositories.conversation import ConversationRepository
from app.repositories.message import MessageRepository
from app.schemas.conversation import ConversationDto
from app.schemas.message import MessageDto
from app.services.ai import AiService
from app.services.balance import BalanceService

logger = logging.getLogger("app.chat")

# 每条对话扣减的积分（与 Java 版一致）
COST_PER_MESSAGE = 10

# 会话标题截断长度（Java `text.substring(0, 20)`）
TITLE_MAX_LENGTH = 20


@dataclass(frozen=True, slots=True)
class ChatPayload:
    """`POST /api/agents/chat/agents` 请求体。"""

    agent_id: str
    text: str
    conversation_id: str | None = None
    message_id: str | None = None
    parent_message_id: str | None = None

    @classmethod
    def parse(cls, body: dict[str, Any]) -> ChatPayload:
        """按 Java `String.valueOf` 的宽松规则取值并校验必填项。"""

        def as_str(key: str) -> str | None:
            value = body.get(key)
            return None if value is None else str(value)

        agent_id = as_str("agent_id")
        text = as_str("text")
        if agent_id is None or not agent_id.strip() or text is None or not text.strip():
            raise ApiException(400, "agent_id and text are required")
        return cls(
            agent_id=agent_id,
            text=text,
            conversation_id=as_str("conversationId"),
            message_id=as_str("messageId"),
            parent_message_id=as_str("parentMessageId"),
        )


@dataclass(frozen=True, slots=True)
class _ConversationRef:
    id: int
    public_id: str
    dto: ConversationDto


def sse_event(event: str, data: Any) -> str:
    """按 Spring `SseEmitter` 风格拼一帧：``event:...\\ndata:{json}\\n\\n``。"""

    payload = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
    return f"event:{event}\ndata:{payload}\n\n"


def dto_json(dto: Any) -> dict[str, Any]:
    """DTO 按 Jackson `non_null` + 别名的规则转成 dict。"""

    return dto.model_dump(mode="json", by_alias=True, exclude_none=True)


class ChatService:
    def __init__(
        self,
        session_factory: async_sessionmaker[AsyncSession] = SessionLocal,
        ai_service: AiService | None = None,
    ) -> None:
        self._session_factory = session_factory
        self._ai = ai_service or AiService()

    async def require_agent(self, public_id: str) -> Agent:
        """推流前查智能体（短事务，查完即关）；不存在 404。"""

        async with self._session_factory() as session:
            agent = await AgentRepository(session).find_by_public_id(public_id)
        if agent is None:
            raise ApiException(404, f"agent not found: {public_id}")
        return agent

    async def stream_chat(
        self,
        user_id: int,
        agent: Agent,
        payload: ChatPayload,
        request: Request,
    ) -> AsyncIterator[str]:
        """SSE 事件流：conversation → message... → final（失败发 error）。"""

        try:
            async for frame in self._run(user_id, agent, payload, request):
                yield frame
        except asyncio.CancelledError:
            # 客户端断开时 Starlette 会取消响应任务；上游已在 _run 的 finally 关闭
            raise
        except Exception as exc:
            logger.exception("SSE 对话失败 user=%s agent=%s", user_id, payload.agent_id)
            message = str(exc) or "internal error"
            yield sse_event("error", {"message": message})

    async def _run(
        self,
        user_id: int,
        agent: Agent,
        payload: ChatPayload,
        request: Request,
    ) -> AsyncIterator[str]:
        # 3. 复用 / 新建会话
        conversation, is_new = await self._resolve_conversation(user_id, agent, payload)

        # 4. 新会话先建立流，前端据此拿到 conversationId
        if is_new:
            yield sse_event("conversation", dto_json(conversation.dto))

        # 5 + 6. 扣积分、落库用户消息（同一短事务）
        user_message_id = payload.message_id or str(uuid.uuid4())
        await self._save_user_turn(user_id, conversation.id, user_message_id, payload)

        # 7. 同会话历史（排除本次用户消息，id 升序）
        history = await self._load_history(conversation.id, user_message_id)

        # 8. 流式调用 DeepSeek，逐段转发
        assistant_message_id = str(uuid.uuid4())
        chunks: list[str] = []
        upstream = self._ai.stream_reply(agent, payload.text, history)
        disconnected = False
        try:
            async for chunk in upstream:
                if await request.is_disconnected():
                    disconnected = True
                    break
                if not chunk:
                    continue
                chunks.append(chunk)
                yield sse_event(
                    "message",
                    {
                        "messageId": assistant_message_id,
                        "conversationId": conversation.public_id,
                        "text": chunk,
                    },
                )
        finally:
            await _aclose(upstream)

        # 客户端已断开：不落库、不发 final
        if disconnected or await request.is_disconnected():
            return

        # 9. 落库助手消息 + 更新会话时间（同一短事务）
        assistant = await self._save_assistant_turn(
            conversation.id,
            conversation.public_id,
            assistant_message_id,
            user_message_id,
            "".join(chunks),
        )

        # 10. final
        yield sse_event("final", dto_json(assistant))

    async def _resolve_conversation(
        self, user_id: int, agent: Agent, payload: ChatPayload
    ) -> tuple[_ConversationRef, bool]:
        """按 conversationId + userId 查找；查不到（含越权）就新建。"""

        async with self._session_factory() as session:
            async with session.begin():
                repo = ConversationRepository(session)
                conversation: Conversation | None = None
                if payload.conversation_id is not None:
                    conversation = await repo.find_by_public_id_and_user_id(
                        payload.conversation_id, user_id
                    )
                is_new = conversation is None
                if conversation is None:
                    now = datetime.now()
                    conversation = Conversation(
                        public_id=str(uuid.uuid4()),
                        user_id=user_id,
                        agent_id=agent.id,
                        title=payload.text[:TITLE_MAX_LENGTH],
                        endpoint="agents",
                        created_at=now,
                        updated_at=now,
                    )
                    await repo.save(conversation)
                dto = ConversationDto.from_entity(conversation)
            dto.agent_id = agent.public_id
        return _ConversationRef(conversation.id, conversation.public_id, dto), is_new

    async def _save_user_turn(
        self,
        user_id: int,
        conversation_id: int,
        user_message_id: str,
        payload: ChatPayload,
    ) -> None:
        async with self._session_factory() as session, session.begin():
            # 余额不足也继续（静默扣到 0）
            await BalanceService(session).deduct(user_id, COST_PER_MESSAGE)
            await MessageRepository(session).save(
                Message(
                    public_id=user_message_id,
                    conversation_id=conversation_id,
                    parent_message_id=payload.parent_message_id,
                    sender="user",
                    text=payload.text,
                    error=False,
                    created_at=datetime.now(),
                )
            )

    async def _load_history(self, conversation_id: int, user_message_id: str) -> list[Message]:
        async with self._session_factory() as session:
            rows = await MessageRepository(session).list_by_conversation_id(conversation_id)
        return [row for row in rows if row.public_id != user_message_id]

    async def _save_assistant_turn(
        self,
        conversation_id: int,
        conversation_public_id: str,
        assistant_message_id: str,
        user_message_id: str,
        reply: str,
    ) -> MessageDto:
        assistant = Message(
            public_id=assistant_message_id,
            conversation_id=conversation_id,
            parent_message_id=user_message_id,
            sender="agent",
            text=reply,
            error=False,
            created_at=datetime.now(),
        )
        async with self._session_factory() as session, session.begin():
            await MessageRepository(session).save(assistant)
            conversation_repo = ConversationRepository(session)
            conversation = await conversation_repo.get(conversation_id)
            if conversation is not None:
                await conversation_repo.update(conversation, updated_at=datetime.now())
        return MessageDto.from_entity(assistant, conversation_public_id)


async def _aclose(stream: AsyncIterator[str]) -> None:
    """尽力关闭上游生成器（触发其 finally 释放 HTTP 连接）。"""

    aclose = getattr(stream, "aclose", None)
    if aclose is None:
        return
    try:
        await aclose()
    except Exception:
        logger.debug("关闭上游流失败", exc_info=True)
