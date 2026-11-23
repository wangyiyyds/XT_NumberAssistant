"""SSE 聊天路由（对应 Java `ChatController`）。

`POST /api/agents/chat/agents` 返回 `text/event-stream`：
conversation / message / final / error 四类事件，字段与 Java 版逐一对齐。
"""

from typing import Any

from fastapi import Depends, Request
from fastapi.responses import StreamingResponse

from app.api.route import create_router
from app.core.config import settings
from app.core.deps import CurrentUser, SessionDep, get_current_user
from app.core.exceptions import ApiException
from app.services.chat import ChatPayload, ChatService

router = create_router(
    prefix="/api",
    tags=["chat"],
    dependencies=[Depends(get_current_user)],
)

# Spring SseEmitter 同款响应头；X-Accel-Buffering 关闭 Nginx 缓冲
SSE_HEADERS = {
    "Cache-Control": "no-cache",
    "X-Accel-Buffering": "no",
}


@router.post("/agents/chat/agents")
async def chat(
    body: dict[str, Any],
    request: Request,
    current_user: CurrentUser,
    session: SessionDep,
) -> StreamingResponse:
    # 1. 参数校验（400）
    payload = ChatPayload.parse(body)

    # 结束鉴权查询留下的只读事务，避免整个流式期间占着数据库连接
    await session.commit()

    # 2. 智能体不存在（404）
    service = ChatService()
    agent = await service.require_agent(payload.agent_id)

    # 缺少密钥时在开始推流前返回明确的 JSON 错误
    if not settings.deepseek_api_key.strip():
        raise ApiException(500, "DEEPSEEK_API_KEY 未配置，无法调用大模型")

    return StreamingResponse(
        service.stream_chat(current_user.id, agent, payload, request),
        media_type="text/event-stream; charset=utf-8",
        headers=SSE_HEADERS,
    )
