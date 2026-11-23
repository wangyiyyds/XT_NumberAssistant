"""认证路由（对应 Java `AuthController`）。"""

from fastapi import Depends

from app.api.route import create_router
from app.core.deps import CurrentUser, SessionDep, get_current_user
from app.schemas.auth import AuthResponse, LoginRequest, RefreshResponse, RegisterRequest
from app.schemas.common import Result
from app.services.auth import AuthService

# 免鉴权：登录 / 注册
public_router = create_router(prefix="/api/auth", tags=["auth"])
# 需鉴权：刷新 / 登出
protected_router = create_router(
    prefix="/api/auth",
    tags=["auth"],
    dependencies=[Depends(get_current_user)],
)


@public_router.post("/login", response_model=Result[AuthResponse])
async def login(payload: LoginRequest, session: SessionDep) -> Result[AuthResponse]:
    return Result.ok(await AuthService(session).login(payload.email, payload.password))


@public_router.post("/register", response_model=Result[AuthResponse])
async def register(payload: RegisterRequest, session: SessionDep) -> Result[AuthResponse]:
    return Result.ok(
        await AuthService(session).register(
            payload.email, payload.password, payload.name, payload.brand
        )
    )


@protected_router.post("/refresh", response_model=Result[RefreshResponse])
async def refresh(current_user: CurrentUser) -> Result[RefreshResponse]:
    token = AuthService.refresh_token(current_user.id)
    return Result.ok(RefreshResponse(token=token))


@protected_router.post("/logout", response_model=Result[None])
async def logout() -> Result[None]:
    return Result.ok(None)
