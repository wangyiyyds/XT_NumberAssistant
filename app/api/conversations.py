"""会话路由（对应 Java `ConversationController`）。"""

from fastapi import Depends, Query

from app.api.route import create_router
from app.core.deps import CurrentUser, SessionDep, get_current_user
from app.schemas.common import Result, SuccessResponse
from app.schemas.conversation import (
    ConversationDto,
    ConversationListResponse,
    RenameConversationRequest,
)
from app.schemas.message import MessageDto
from app.services.conversation import ConversationService

router = create_router(
    prefix="/api/convos",
    tags=["conversations"],
    dependencies=[Depends(get_current_user)],
)


@router.get("", response_model=Result[ConversationListResponse])
async def list_conversations(
    current_user: CurrentUser,
    session: SessionDep,
    agent_id: str | None = Query(default=None, alias="agent_id"),
) -> Result[ConversationListResponse]:
    return Result.ok(await ConversationService(session).list(current_user.id, agent_id))


@router.get("/{public_id}/messages", response_model=Result[list[MessageDto]])
async def messages(
    public_id: str, current_user: CurrentUser, session: SessionDep
) -> Result[list[MessageDto]]:
    return Result.ok(await ConversationService(session).messages(current_user.id, public_id))


@router.patch("/{public_id}", response_model=Result[ConversationDto])
async def rename(
    public_id: str,
    payload: RenameConversationRequest,
    current_user: CurrentUser,
    session: SessionDep,
) -> Result[ConversationDto]:
    return Result.ok(
        await ConversationService(session).rename(current_user.id, public_id, payload.title)
    )


@router.delete("/{public_id}", response_model=Result[SuccessResponse])
async def delete(
    public_id: str, current_user: CurrentUser, session: SessionDep
) -> Result[SuccessResponse]:
    return Result.ok(await ConversationService(session).delete(current_user.id, public_id))
