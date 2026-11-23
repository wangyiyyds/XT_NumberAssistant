"""部门 / 智能体路由（对应 Java `AgentController`）。"""

from typing import Any

from fastapi import Depends, Query

from app.api.route import create_router
from app.core.deps import SessionDep, get_current_user
from app.schemas.agent import AgentAccessResponse, AgentDto
from app.schemas.common import PageResult, Result
from app.schemas.department import DepartmentDto
from app.services.agent import AgentService

router = create_router(
    prefix="/api",
    tags=["agents"],
    dependencies=[Depends(get_current_user)],
)


@router.get("/departments", response_model=Result[list[DepartmentDto]])
async def departments(session: SessionDep) -> Result[list[DepartmentDto]]:
    return Result.ok(await AgentService(session).list_departments())


@router.get("/agents", response_model=Result[PageResult[AgentDto]])
async def agents(
    session: SessionDep,
    department: str | None = None,
    is_preset: str | None = Query(default=None, alias="isPreset"),
    required_permission: str | None = Query(default=None, alias="requiredPermission"),
) -> Result[PageResult[AgentDto]]:
    # requiredPermission 与 Java 版一致：接收但不参与过滤
    return Result.ok(await AgentService(session).list_agents(department, is_preset))


@router.get("/agents/my-access", response_model=Result[AgentAccessResponse])
async def my_access(session: SessionDep) -> Result[AgentAccessResponse]:
    return Result.ok(await AgentService(session).my_access())


@router.get("/agents/chat/active", response_model=Result[list[Any]])
async def chat_active() -> Result[list[Any]]:
    return Result.ok([])


@router.get("/agents/{public_id}", response_model=Result[AgentDto])
async def agent_detail(public_id: str, session: SessionDep) -> Result[AgentDto]:
    return Result.ok(await AgentService(session).detail(public_id))
