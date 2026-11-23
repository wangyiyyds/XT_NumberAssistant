"""基础设施 / 站点配置路由（对应 Java `CoreController`）。"""

from typing import Any

from fastapi import Depends

from app.api.route import create_router
from app.core.config import settings
from app.core.deps import CurrentUser, SessionDep, get_current_user
from app.schemas.balance import BalanceDto
from app.schemas.common import Result
from app.schemas.user import UserDto
from app.services.balance import BalanceService

# `/api/config` 免鉴权
public_router = create_router(prefix="/api", tags=["core"])
# 其余 /api/** 需要 Authorization: Bearer <token>
protected_router = create_router(
    prefix="/api",
    tags=["core"],
    dependencies=[Depends(get_current_user)],
)


@public_router.get("/config", response_model=Result[dict[str, Any]])
async def config() -> Result[dict[str, Any]]:
    return Result.ok(
        {
            "appTitle": "先t霆AI超级系统",
            "registrationEnabled": True,
            "emailLoginEnabled": True,
            "socialLogins": [],
            "ldap": {"enabled": False},
            "balance": {
                "enabled": True,
                "autoRefillEnabled": True,
                "startBalance": settings.app_balance_start,
            },
            "conversationImportMaxFileSize": "20mb",
        }
    )


@protected_router.get("/user", response_model=Result[UserDto])
async def user(current_user: CurrentUser) -> Result[UserDto]:
    return Result.ok(UserDto.from_entity(current_user))


@protected_router.get("/balance", response_model=Result[BalanceDto])
async def balance(current_user: CurrentUser, session: SessionDep) -> Result[BalanceDto]:
    return Result.ok(await BalanceService(session).get_balance(current_user))


@protected_router.get("/notifications", response_model=Result[dict[str, Any]])
async def notifications() -> Result[dict[str, Any]]:
    return Result.ok({"items": [], "total": 0, "preferences": {"muted": False}})


@protected_router.get("/roles/user", response_model=Result[dict[str, Any]])
async def roles() -> Result[dict[str, Any]]:
    return Result.ok(
        {
            "role": "USER",
            "name": "User",
            "permissions": {"AGENTS": {"CREATE": True, "SHARE": False, "SHARE_PUBLIC": False}},
        }
    )


@protected_router.get("/models", response_model=Result[dict[str, Any]])
async def models() -> Result[dict[str, Any]]:
    return Result.ok(
        {
            "agents": ["先霆默认模型"],
            "DeepSeek": ["deepseek-chat", "deepseek-reasoner"],
        }
    )


@protected_router.get("/search/enable", response_model=Result[bool])
async def search_enabled() -> Result[bool]:
    return Result.ok(False)


@protected_router.get("/endpoints", response_model=Result[dict[str, Any]])
async def endpoints() -> Result[dict[str, Any]]:
    return Result.ok({"agents": True})


@protected_router.get("/banner", response_model=Result[dict[str, Any]])
async def banner() -> Result[dict[str, Any]]:
    return Result.ok({"show": False})
