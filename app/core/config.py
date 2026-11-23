"""应用配置。

P2 补充认证与日志配置；P4 补充 DeepSeek 配置。
"""

from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

BASE_DIR = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    """从环境变量 / 项目根目录 `.env` 读取配置。"""

    model_config = SettingsConfigDict(
        env_file=BASE_DIR / ".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    app_env: str = "dev"

    # JWT（与 Java 版同一密钥，旧 token 继续有效）
    app_jwt_secret: str = "xianting-chat-clone-secret-key-change-in-production"
    app_jwt_expire_hours: int = 168

    # 新用户初始积分
    app_balance_start: int = 100000

    # DeepSeek（缺失不阻止启动，调用聊天接口时返回明确错误）
    deepseek_api_key: str = ""
    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_model: str = "deepseek-chat"
    deepseek_temperature: float = 0.7

    log_level: str = "DEBUG"

    # PostgreSQL（compose 暴露在 5433）
    database_url: str = "postgresql+asyncpg://xianting:xianting@localhost:5433/xianting_chat"

    # 连接池（可配置）
    db_echo: bool = False
    db_pool_size: int = 10
    db_max_overflow: int = 20
    db_pool_timeout: float = 30.0
    db_pool_recycle: int = 1800


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
