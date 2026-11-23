"""异步数据库引擎与会话工厂。"""

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.orm import SessionTransactionOrigin

from app.core.config import settings


def build_engine(url: str | None = None) -> AsyncEngine:
    """按配置创建异步引擎。

    传入 `url` 时用于测试库等场景，否则使用 `DATABASE_URL`。
    """

    return create_async_engine(
        url or settings.database_url,
        echo=settings.db_echo,
        pool_pre_ping=True,
        pool_size=settings.db_pool_size,
        max_overflow=settings.db_max_overflow,
        pool_timeout=settings.db_pool_timeout,
        pool_recycle=settings.db_pool_recycle,
    )


engine: AsyncEngine = build_engine()

SessionLocal = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False,
)


async def get_session() -> AsyncIterator[AsyncSession]:
    """FastAPI 依赖：每个请求一个 AsyncSession，退出时自动关闭。"""

    async with SessionLocal() as session:
        yield session


@asynccontextmanager
async def atomic(session: AsyncSession) -> AsyncIterator[None]:
    """把多个写操作包成一个事务。

    `get_current_user` 等只读查询会以 AUTOBEGIN 方式开启事务；这里先结束这种
    只读事务，再显式开启业务事务，保证整段写入原子提交。若调用方已显式开启事务，
    则直接加入，由外层负责提交。
    """

    current = session.sync_session.get_transaction()
    if current is None:
        async with session.begin():
            yield
    elif current.origin is SessionTransactionOrigin.AUTOBEGIN:
        await session.commit()
        async with session.begin():
            yield
    else:
        yield
