"""部门仓储（对应 Java `DepartmentRepository`）。"""

from sqlalchemy import or_, select

from app.models.department import Department
from app.repositories.base import BaseRepository


class DepartmentRepository(BaseRepository[Department]):
    model = Department

    async def list_all(self) -> list[Department]:
        """全部部门（无排序，与 Java `selectList(null)` 一致）。"""

        result = await self.session.scalars(select(Department))
        return list(result.all())

    async def list_all_order_by_sort_order(self) -> list[Department]:
        """全部部门，按 sort_order 升序。"""

        result = await self.session.scalars(select(Department).order_by(Department.sort_order))
        return list(result.all())

    async def find_by_name_or_public_id(self, keyword: str) -> Department | None:
        return await self.session.scalar(
            select(Department).where(
                or_(Department.name == keyword, Department.public_id == keyword)
            )
        )
