"""UserDto（对应 Java `dto/UserDto`）。"""

from pydantic import BaseModel, ConfigDict, Field

from app.models.user import User
from app.schemas.types import JavaDateTime


class UserDto(BaseModel):
    """对外用户字段：数据库自增 id 不外露，`_id` 使用 publicId。"""

    model_config = ConfigDict(populate_by_name=True)

    id: str = Field(alias="_id")
    brand: str
    email: str
    name: str | None = None
    avatar: str | None = None
    email_verified: bool = Field(alias="emailVerified")
    disabled: bool
    contract_read_at: JavaDateTime | None = Field(default=None, alias="contractReadAt")
    created_at: JavaDateTime = Field(alias="createdAt")

    @classmethod
    def from_entity(cls, user: User) -> "UserDto":
        return cls(
            id=user.public_id,
            brand=user.brand,
            email=user.email,
            name=user.name,
            avatar=user.avatar,
            email_verified=user.email_verified,
            disabled=user.disabled,
            contract_read_at=user.contract_read_at,
            created_at=user.created_at,
        )
