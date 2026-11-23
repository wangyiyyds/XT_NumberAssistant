"""数据库初始化：用 asyncpg 执行 `db/schema.sql` 与 `db/data.sql`。

生产/测试建表统一以 SQL 脚本为准，不使用 `Base.metadata.create_all()`。
"""

from pathlib import Path

import asyncpg
from sqlalchemy.engine import make_url

from app.core.config import settings

PROJECT_ROOT = Path(__file__).resolve().parents[2]
DB_DIR = PROJECT_ROOT / "db"
SCHEMA_FILE = DB_DIR / "schema.sql"
DATA_FILE = DB_DIR / "data.sql"


def to_asyncpg_url(url: str) -> str:
    """把 SQLAlchemy URL（postgresql+asyncpg://）转成 asyncpg 可用的 DSN。"""

    return make_url(url).set(drivername="postgresql").render_as_string(hide_password=False)


async def ensure_database(url: str) -> bool:
    """确保目标数据库存在；返回是否新建。

    通过同一实例的 `postgres` 库执行 `CREATE DATABASE`。
    """

    parsed = make_url(url)
    database = parsed.database
    if not database:
        raise ValueError("DATABASE_URL 缺少数据库名")
    if not database.replace("_", "").isalnum() or database[0].isdigit():
        raise ValueError(f"非法数据库名: {database}")

    admin_dsn = parsed.set(drivername="postgresql", database="postgres").render_as_string(
        hide_password=False
    )
    conn = await asyncpg.connect(admin_dsn)
    try:
        exists = await conn.fetchval("SELECT 1 FROM pg_database WHERE datname = $1", database)
        if exists:
            return False
        # 数据库名无法参数化，已在上方做白名单校验
        await conn.execute(f'CREATE DATABASE "{database}"')
        return True
    finally:
        await conn.close()


async def init_database(
    database_url: str | None = None,
    *,
    create_database: bool = False,
    include_data: bool = True,
) -> None:
    """执行 schema.sql（可选 data.sql）。

    `schema.sql` 自带事务与 `DROP TABLE IF EXISTS`，重复执行会重置目标库。
    """

    url = database_url or settings.database_url
    if create_database:
        await ensure_database(url)

    dsn = to_asyncpg_url(url)
    conn = await asyncpg.connect(dsn)
    try:
        await conn.execute(SCHEMA_FILE.read_text(encoding="utf-8"))
        if include_data:
            await conn.execute(DATA_FILE.read_text(encoding="utf-8"))
    finally:
        await conn.close()
