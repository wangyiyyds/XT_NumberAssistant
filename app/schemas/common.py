"""统一响应外壳与分页结构。

与 Java 版 `common/Result` 逐字段对齐：

- 成功：``{"code": 200, "message": "操作成功", "data": ..., "timestamp": "..."}``
- 失败：HTTP 状态码与 ``code`` 一致，``data`` 为 null 且不序列化。
- 全局 ``exclude_none`` 对齐 Jackson ``default-property-inclusion: non_null``。
"""

from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.types import JavaDateTime


class Result[T](BaseModel):
    """所有 REST JSON 接口的统一外壳。"""

    model_config = ConfigDict(exclude_none=True)

    code: int
    message: str
    data: T | None = None
    timestamp: JavaDateTime = Field(default_factory=datetime.now)

    @classmethod
    def ok(cls, data: T | None = None) -> "Result[T]":
        return cls(code=200, message="操作成功", data=data)

    @classmethod
    def fail(cls, code: int, message: str) -> "Result[None]":
        return cls(code=code, message=message, data=None)


class SuccessResponse(BaseModel):
    """删除类接口的 data 结构：{"success": true}。"""

    success: bool = True


class PageResult[T](BaseModel):
    """OpenAI 风格分页列表（智能体列表等）。"""

    model_config = ConfigDict(exclude_none=True)

    object: str = "list"
    data: list[T]
    first_id: str | None = None
    last_id: str | None = None
    has_more: bool = False
    after: str | None = None


def serialize_result(result: Result[Any]) -> dict[str, Any]:
    """异常处理器等非路由场景下按外壳规则序列化。"""

    return result.model_dump(mode="json", by_alias=True, exclude_none=True)
