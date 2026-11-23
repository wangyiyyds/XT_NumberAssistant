"""Java `LocalDateTime` 兼容序列化。

Java `LocalDateTime.toString()` 的输出规则：
- 无时区、`T` 分隔；
- 秒位始终输出（本项目约定）；
- 小数位按需：整毫秒输出 3 位，其余输出 6 位（Python 只有微秒精度，
  对应 Java 的毫秒组 / 微秒组）。
"""

from datetime import datetime
from typing import Annotated

from pydantic import PlainSerializer


def format_local_datetime(value: datetime) -> str:
    """按 Java `LocalDateTime.toString()` 风格格式化。"""

    base = (
        f"{value.year:04d}-{value.month:02d}-{value.day:02d}"
        f"T{value.hour:02d}:{value.minute:02d}:{value.second:02d}"
    )
    micro = value.microsecond
    if micro == 0:
        return base
    if micro % 1000 == 0:
        return f"{base}.{micro // 1000:03d}"
    return f"{base}.{micro:06d}"


# 用在 DTO / Result.timestamp 上；json 序列化时输出 Java 风格字符串
JavaDateTime = Annotated[
    datetime,
    PlainSerializer(format_local_datetime, return_type=str, when_used="json"),
]
