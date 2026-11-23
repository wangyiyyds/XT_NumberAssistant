"""业务异常与全局异常处理器（对应 Java `ApiException` + `GlobalExceptionHandler`）。

所有失败响应都使用统一外壳，HTTP 状态码与 ``code`` 一致，``data`` 不序列化。
"""

import logging
from typing import Any

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.schemas.common import Result, serialize_result

logger = logging.getLogger("app.error")


class ApiException(Exception):
    """带 HTTP 状态码的业务异常。"""

    def __init__(self, status: int, message: str) -> None:
        super().__init__(message)
        self.status = status
        self.message = message


def _fail_response(status: int, message: str) -> JSONResponse:
    result = Result.fail(status, message)
    return JSONResponse(status_code=status, content=serialize_result(result))


def register_exception_handlers(app: FastAPI) -> None:
    """注册全局处理器，保证任何失败都返回统一外壳。"""

    @app.exception_handler(ApiException)
    async def handle_api_exception(request: Request, exc: ApiException) -> JSONResponse:
        return _fail_response(exc.status, exc.message)

    @app.exception_handler(RequestValidationError)
    async def handle_validation_error(
        request: Request, exc: RequestValidationError
    ) -> JSONResponse:
        logger.debug("请求参数校验失败 %s: %s", request.url.path, exc.errors())
        return _fail_response(400, "请求参数错误")

    @app.exception_handler(StarletteHTTPException)
    async def handle_http_exception(request: Request, exc: StarletteHTTPException) -> JSONResponse:
        return _fail_response(exc.status_code, str(exc.detail))

    @app.exception_handler(Exception)
    async def handle_unexpected(request: Request, exc: Exception) -> JSONResponse:
        logger.exception("未处理异常 %s %s", request.method, request.url.path)
        message: Any = str(exc) or "internal error"
        return _fail_response(500, str(message))
