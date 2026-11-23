"""FastAPI 应用入口。

P2：注册 CORS、请求日志、全局异常处理、Swagger（`/docs`、`/redoc`、
`/swagger-ui.html` 别名）以及 auth / core 路由。
"""

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from typing import Any

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.openapi.utils import get_openapi
from fastapi.responses import RedirectResponse

from app.api import api_router
from app.core.exceptions import register_exception_handlers
from app.core.logging import RequestLoggingMiddleware, setup_logging

setup_logging()


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    # 后续轮次在此初始化 / 释放外部资源
    yield


app = FastAPI(
    title="先霆自媒体获客系统",
    version="0.1.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
)

# 日志在内层、CORS 在外层：异常响应也能带上 CORS 头
app.add_middleware(RequestLoggingMiddleware)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    # Java 版：GET/POST/PUT/DELETE/OPTIONS，补上遗漏的 PATCH
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"],
    allow_headers=["*"],
)

register_exception_handlers(app)
app.include_router(api_router)


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/swagger-ui.html", include_in_schema=False)
async def swagger_ui_alias() -> RedirectResponse:
    """保持 Java 版的 Swagger UI 访问习惯。"""

    return RedirectResponse(url="/docs")


def _custom_openapi() -> dict[str, Any]:
    """与 Java `OpenApiConfig` 一致：全局 Bearer 认证。"""

    if app.openapi_schema:
        return app.openapi_schema

    schema = get_openapi(
        title=app.title,
        version=app.version,
        description=app.description,
        routes=app.routes,
    )
    components = schema.setdefault("components", {})
    components.setdefault("securitySchemes", {})["bearerAuth"] = {
        "type": "http",
        "scheme": "bearer",
        "bearerFormat": "JWT",
    }
    schema["security"] = [{"bearerAuth": []}]
    app.openapi_schema = schema
    return schema


app.openapi = _custom_openapi  # type: ignore[method-assign]
