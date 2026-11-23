"""智能体仓储（对应 Java `AgentRepository`）。"""

from sqlalchemy import select

from app.models.agent import Agent
from app.repositories.base import BaseRepository


class AgentRepository(BaseRepository[Agent]):
    model = Agent

    async def list_all(self) -> list[Agent]:
        """全部智能体，按 id 升序。"""

        result = await self.session.scalars(select(Agent).order_by(Agent.id))
        return list(result.all())

    async def list_by_department(self, department_id: int | None, preset_only: bool) -> list[Agent]:
        """按部门过滤 + 是否仅预置，按 id 升序；department_id 为 None 表示不过滤。"""

        stmt = select(Agent)
        if department_id is not None:
            stmt = stmt.where(Agent.department_id == department_id)
        if preset_only:
            stmt = stmt.where(Agent.is_preset.is_(True))
        stmt = stmt.order_by(Agent.id)
        result = await self.session.scalars(stmt)
        return list(result.all())

    async def find_by_public_id(self, public_id: str) -> Agent | None:
        return await self.session.scalar(select(Agent).where(Agent.public_id == public_id))
