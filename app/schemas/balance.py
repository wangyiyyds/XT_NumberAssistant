"""BalanceDto（对应 Java `dto/BalanceDto`）。"""

from pydantic import BaseModel, ConfigDict, Field

from app.models.balance import Balance
from app.schemas.types import JavaDateTime


class BalanceDto(BaseModel):
    """余额不存在时返回 0 积分，不报错（与 Java 转换器兜底一致）。"""

    model_config = ConfigDict(populate_by_name=True)

    token_credits: int = Field(alias="tokenCredits")
    auto_refill_enabled: bool = Field(alias="autoRefillEnabled")
    expire_at: JavaDateTime | None = Field(default=None, alias="expireAt")
    user: str

    @classmethod
    def from_entity(cls, balance: Balance | None, user_public_id: str) -> "BalanceDto":
        return cls(
            token_credits=balance.token_credits if balance is not None else 0,
            auto_refill_enabled=bool(balance is not None and balance.auto_refill_enabled),
            expire_at=balance.expire_at if balance is not None else None,
            user=user_public_id,
        )
