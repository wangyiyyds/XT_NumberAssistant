"""DepartmentDto（对应 Java `dto/DepartmentDto`）。"""

from pydantic import BaseModel, ConfigDict, Field

from app.models.department import Department


class DepartmentDto(BaseModel):
    """部门对外字段；`extraTools` 恒为 null（序列化时省略），`__v` 恒为 0。"""

    model_config = ConfigDict(populate_by_name=True)

    id: str = Field(alias="_id")
    name: str
    enabled: bool
    brand_only: bool = Field(alias="brandOnly")
    sort_order: int = Field(alias="sortOrder")
    extra_tools: list[str] | None = Field(default=None, alias="extraTools")
    v: int = Field(default=0, alias="__v")

    @classmethod
    def from_entity(cls, department: Department) -> "DepartmentDto":
        return cls(
            id=department.public_id,
            name=department.name,
            enabled=department.enabled,
            brand_only=department.brand_only,
            sort_order=department.sort_order,
        )
