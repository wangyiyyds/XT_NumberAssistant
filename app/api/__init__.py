"""API 路由汇总。

鉴权分组约定：
- ``public_router``：免鉴权（login / register / config）
- ``protected_router``：统一带 ``get_current_user`` 依赖

后续轮次新增的业务 router 必须挂到 ``protected_router``（或自行声明同等依赖），
不要把需要鉴权的路由直接加到 `api_router`。
"""

from fastapi import APIRouter

from app.api import agents, auth, chat, conversations, core, inspiration

api_router = APIRouter()

api_router.include_router(auth.public_router)
api_router.include_router(core.public_router)
api_router.include_router(auth.protected_router)
api_router.include_router(core.protected_router)
api_router.include_router(agents.router)
api_router.include_router(chat.router)
api_router.include_router(conversations.router)
api_router.include_router(inspiration.router)

__all__ = ["api_router"]
