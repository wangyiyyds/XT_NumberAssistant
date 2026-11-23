"""SQLAlchemy 声明式基类。

模型只映射既有 `db/schema.sql` 的表结构，生产建表始终以 SQL 脚本为准，
不调用 `Base.metadata.create_all()`，避免两套 DDL 漂移。
"""

from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    """所有 ORM 模型的基类。"""
