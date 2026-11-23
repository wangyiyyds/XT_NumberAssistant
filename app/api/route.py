"""全局响应序列化策略。

Java 版 Jackson 配置了 ``default-property-inclusion: non_null``；这里通过
自定义 `APIRoute` 让所有路由默认 ``response_model_exclude_none=True``，
等价地省略 DTO 中值为 null 的字段（如 avatar / contractReadAt / data）。
"""

from typing import Any

from fastapi import APIRouter
from fastapi.routing import APIRoute


class ExcludeNoneRoute(APIRoute):
    def __init__(self, *args: Any, **kwargs: Any) -> None:
        # 装饰器会显式传入默认值 False，这里必须覆盖而不是 setdefault
        kwargs["response_model_exclude_none"] = True
        super().__init__(*args, **kwargs)


def create_router(**kwargs: Any) -> APIRouter:
    """创建默认省略 null 字段的 router。"""

    kwargs.setdefault("route_class", ExcludeNoneRoute)
    return APIRouter(**kwargs)
