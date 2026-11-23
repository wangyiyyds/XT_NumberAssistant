"""FastAPI 依赖：替代 Java 的 `UserContext` + `AuthInterceptor`。

放行策略由路由分组实现（`app/api/__init__.py`）：

- 公开：`POST /api/auth/login`、`POST /api/auth/register`、`GET /api/config`、`/health`
- 其余 `/api/**` 统一挂在带 `get_current_user` 依赖的 router 上
- OPTIONS 预检请求由 CORS 中间件直接应答，不会进入依赖

失败返回 401 + 统一外壳，message 与 Java 版一致。
"""

from typing import Annotated

from fastapi import Depends, Request
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.exceptions import ApiException
from app.core.security import verify_token
from app.db.session import get_session
from app.models.user import User
from app.repositories.user import UserRepository

__all__ = ["CurrentUser", "SessionDep", "get_current_user", "get_session"]


def _bearer_token(request: Request) -> str | None:
    header = request.headers.get("Authorization")
    if header is not None and header.startswith("Bearer "):
        return header[7:]
    return None


async def get_current_user(
    request: Request,
    session: Annotated[AsyncSession, Depends(get_session)],
) -> User:
    """校验 Bearer token 并加载当前用户。"""

    user_id = verify_token(_bearer_token(request), settings.app_jwt_secret)
    if user_id is None:
        raise ApiException(401, "invalid or expired token")

    user = await UserRepository(session).get(user_id)
    if user is None:
        raise ApiException(401, "user not found")
    return user


# 路由层复用的依赖别名
SessionDep = Annotated[AsyncSession, Depends(get_session)]
CurrentUser = Annotated[User, Depends(get_current_user)]
