"""Repository 基类。

事务约定：
- 读请求直接查询，不提交事务。
- 写方法（`save` / `update` / `remove` / `delete_*`）在无显式事务时，
  使用 `async with session.begin()` 开启并提交短事务。
- 服务层若要把多个写操作组成一个单元，用 `async with session.begin()` 包住整个单元，
  repository 会加入该事务，不再单独开启。
- AsyncSession 的只读查询会自动开启事务；若同一个 session 先读后写，写方法会随该事务
  一起提交。需要原子性的「读 + 多写」仍应由服务层显式包一层 `async with session.begin()`。
"""

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from typing import Any

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import SessionTransactionOrigin

from app.models.base import Base


class BaseRepository[ModelT: Base]:
    model: type[ModelT]

    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    @asynccontextmanager
    async def _transaction(self) -> AsyncIterator[None]:
        """加入显式事务，或为独立写入开启 / 结束一个短事务。"""

        current = self.session.sync_session.get_transaction()
        if current is not None and current.origin is not SessionTransactionOrigin.AUTOBEGIN:
            # 调用方显式开启了事务（async with session.begin()），由调用方负责提交
            yield
        elif current is not None:
            # 只读查询自动开启的事务：写入后随该事务一起提交
            yield
            await self.session.commit()
        else:
            async with self.session.begin():
                yield

    async def get(self, id_: int) -> ModelT | None:
        return await self.session.get(self.model, id_)

    async def count(self) -> int:
        result = await self.session.scalar(select(func.count()).select_from(self.model))
        return int(result or 0)

    async def save(self, entity: ModelT) -> ModelT:
        """插入实体并 flush（取得自增 id），事务由 `_transaction` 提交。"""

        async with self._transaction():
            self.session.add(entity)
            await self.session.flush()
        return entity

    async def update(self, entity: ModelT, **values: Any) -> ModelT:
        """更新实体字段并 flush。"""

        async with self._transaction():
            for key, value in values.items():
                setattr(entity, key, value)
            await self.session.flush()
        return entity

    async def remove(self, entity: ModelT) -> None:
        """删除实体。"""

        async with self._transaction():
            await self.session.delete(entity)
            await self.session.flush()
