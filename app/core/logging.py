"""日志配置与请求日志中间件（替代 Java `ServiceLogAspect`）。

使用纯 ASGI 中间件而不是 `BaseHTTPMiddleware`，避免包裹 SSE 流式响应时
产生缓冲或提前消费 `receive`。
"""

import logging
import time
from collections.abc import Awaitable, Callable
from typing import Any

from app.core.config import settings

Scope = dict[str, Any]
Message = dict[str, Any]
Receive = Callable[[], Awaitable[Message]]
Send = Callable[[Message], Awaitable[None]]

access_logger = logging.getLogger("app.request")


def setup_logging() -> None:
    """按 `LOG_LEVEL` 初始化根日志（重复调用不会叠加 handler）。"""

    level = logging.getLevelNamesMapping().get(settings.log_level.upper(), logging.INFO)
    logging.basicConfig(
        level=level,
        format="%(asctime)s %(levelname)s %(name)s - %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )


class RequestLoggingMiddleware:
    """记录每个 HTTP 请求的方法、路径、状态码与耗时。"""

    def __init__(self, app: Any) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        method = scope.get("method", "")
        path = scope.get("path", "")
        query = scope.get("query_string", b"").decode("latin-1")
        target = f"{path}?{query}" if query else path
        start = time.perf_counter()
        status_code = 500

        async def send_wrapper(message: Message) -> None:
            nonlocal status_code
            if message["type"] == "http.response.start":
                status_code = int(message["status"])
            await send(message)

        try:
            await self.app(scope, receive, send_wrapper)
        except Exception:
            access_logger.exception("%s %s -> 500", method, target)
            raise
        else:
            cost_ms = (time.perf_counter() - start) * 1000
            access_logger.info("%s %s -> %s cost=%.1fms", method, target, status_code, cost_ms)
