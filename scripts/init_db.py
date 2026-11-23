"""本地 / 测试库初始化。

用法（项目根目录）：
    uv run python scripts/init_db.py
    uv run python scripts/init_db.py --db xianting_chat_test --create

`init_db.py` 使用 `db/schema.sql` + `db/data.sql` 建表并导入种子数据；
重复执行会重置目标库。
"""

from __future__ import annotations

import argparse
import asyncio
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from sqlalchemy.engine import make_url

from app.core.config import settings
from app.db.init import init_database


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="初始化 xianting_chat 数据库")
    parser.add_argument(
        "--url",
        help="覆盖 DATABASE_URL，例如 postgresql+asyncpg://user:pass@host:5433/db",
    )
    parser.add_argument(
        "--db",
        dest="database",
        help="在 DATABASE_URL 基础上覆盖数据库名（本地 / 测试库）",
    )
    parser.add_argument(
        "--create",
        action="store_true",
        help="数据库不存在时先创建",
    )
    parser.add_argument(
        "--schema-only",
        action="store_true",
        help="只执行 schema.sql，不导入 data.sql",
    )
    return parser.parse_args()


def resolve_url(args: argparse.Namespace) -> str:
    url = args.url or settings.database_url
    if args.database:
        url = make_url(url).set(database=args.database).render_as_string(
            hide_password=False
        )
    return url


async def run(args: argparse.Namespace) -> None:
    url = resolve_url(args)
    target = make_url(url)
    print(f"目标数据库: {target.render_as_string(hide_password=True)}")
    await init_database(
        url,
        create_database=args.create,
        include_data=not args.schema_only,
    )
    print("初始化完成")


def main() -> None:
    asyncio.run(run(parse_args()))


if __name__ == "__main__":
    main()