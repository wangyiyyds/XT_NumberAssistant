"""灵感中心路由（对应 Java `InspirationController`）。"""

from fastapi import Depends

from app.api.route import create_router
from app.core.deps import CurrentUser, SessionDep, get_current_user
from app.schemas.common import Result, SuccessResponse
from app.schemas.inspiration import (
    AddKeywordRequest,
    InspirationKeywordDto,
    InspirationListResponse,
)
from app.services.inspiration import InspirationService

router = create_router(
    prefix="/api/inspiration",
    tags=["inspiration"],
    dependencies=[Depends(get_current_user)],
)


@router.get("", response_model=Result[InspirationListResponse])
async def inspiration(
    current_user: CurrentUser, session: SessionDep
) -> Result[InspirationListResponse]:
    return Result.ok(await InspirationService(session).list(current_user.id))


@router.post("/keywords", response_model=Result[InspirationKeywordDto])
async def add_keyword(
    payload: AddKeywordRequest, current_user: CurrentUser, session: SessionDep
) -> Result[InspirationKeywordDto]:
    return Result.ok(
        await InspirationService(session).add_keyword(current_user.id, payload.keyword)
    )


@router.delete("/keywords/{id}", response_model=Result[SuccessResponse])
async def delete_keyword(
    id: int, current_user: CurrentUser, session: SessionDep
) -> Result[SuccessResponse]:
    return Result.ok(await InspirationService(session).delete_keyword(current_user.id, id))


@router.post("/keywords/{id}/refresh", response_model=Result[InspirationKeywordDto])
async def refresh(
    id: int, current_user: CurrentUser, session: SessionDep
) -> Result[InspirationKeywordDto]:
    return Result.ok(await InspirationService(session).refresh(current_user.id, id))
