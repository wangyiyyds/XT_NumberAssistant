"""智能体 / 部门服务（对应 Java `AgentServiceImpl`）。"""

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ApiException
from app.models.agent import Agent
from app.repositories.agent import AgentRepository
from app.repositories.department import DepartmentRepository
from app.schemas.agent import AgentAccessResponse, AgentDto
from app.schemas.common import PageResult
from app.schemas.department import DepartmentDto


class AgentService:
    def __init__(self, session: AsyncSession) -> None:
        self.agents = AgentRepository(session)
        self.departments = DepartmentRepository(session)

    async def list_departments(self) -> list[DepartmentDto]:
        rows = await self.departments.list_all_order_by_sort_order()
        return [DepartmentDto.from_entity(row) for row in rows]

    async def list_agents(
        self, department: str | None, is_preset: str | None
    ) -> PageResult[AgentDto]:
        department_id: int | None = None
        if department is not None and department.strip():
            found = await self.departments.find_by_name_or_public_id(department)
            if found is None:
                raise ApiException(404, f"department not found: {department}")
            department_id = found.id

        preset_only = is_preset == "1" or (is_preset is not None and is_preset.lower() == "true")
        rows = await self.agents.list_by_department(department_id, preset_only)
        department_names = await self._department_names()
        data = [AgentDto.from_entity(row, department_names.get(row.department_id)) for row in rows]
        return PageResult[AgentDto](
            data=data,
            first_id=data[0].id if data else None,
            last_id=data[-1].id if data else None,
        )

    async def my_access(self) -> AgentAccessResponse:
        rows = await self.agents.list_all()
        return AgentAccessResponse(data=[row.public_id for row in rows])

    async def detail(self, public_id: str) -> AgentDto:
        agent = await self.require_by_public_id(public_id)
        department_names = await self._department_names()
        return AgentDto.from_entity(agent, department_names.get(agent.department_id))

    async def require_by_public_id(self, public_id: str) -> Agent:
        agent = await self.agents.find_by_public_id(public_id)
        if agent is None:
            raise ApiException(404, f"agent not found: {public_id}")
        return agent

    async def _department_names(self) -> dict[int, str]:
        rows = await self.departments.list_all()
        return {row.id: row.name for row in rows}
