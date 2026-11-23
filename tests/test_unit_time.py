"""单元测试：时间序列化与统一外壳。

Java `LocalDateTime.toString()` 的格式是前端解析 / 展示的契约。
"""

from __future__ import annotations

import re
from datetime import datetime

from app.schemas.common import PageResult, Result, SuccessResponse, serialize_result
from app.schemas.types import format_local_datetime

TIMESTAMP_RE = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{3}|\.\d{6})?$")


def test_seconds_are_always_present() -> None:
    assert format_local_datetime(datetime(2026, 11, 23, 14, 51, 19)) == "2026-11-23T14:51:19"


def test_zero_microseconds_has_no_fraction() -> None:
    assert format_local_datetime(datetime(2026, 1, 2, 3, 4, 5, 0)) == "2026-01-02T03:04:05"


def test_whole_milliseconds_use_three_digits() -> None:
    assert format_local_datetime(datetime(2026, 1, 2, 3, 4, 5, 123000)) == (
        "2026-01-02T03:04:05.123"
    )


def test_sub_millisecond_uses_six_digits() -> None:
    assert format_local_datetime(datetime(2026, 1, 2, 3, 4, 5, 123456)) == (
        "2026-01-02T03:04:05.123456"
    )


def test_single_microsecond_is_padded() -> None:
    assert format_local_datetime(datetime(2026, 1, 2, 3, 4, 5, 1)) == ("2026-01-02T03:04:05.000001")


def test_result_timestamp_is_java_style() -> None:
    payload = Result.ok(1).model_dump(mode="json")
    assert TIMESTAMP_RE.match(payload["timestamp"]), payload["timestamp"]


def test_result_ok_envelope() -> None:
    payload = Result.ok({"a": 1}).model_dump(mode="json")
    assert payload["code"] == 200
    assert payload["message"] == "操作成功"
    assert payload["data"] == {"a": 1}


def test_result_fail_omits_data() -> None:
    payload = serialize_result(Result.fail(402, "积分不足"))
    assert payload == {
        "code": 402,
        "message": "积分不足",
        "timestamp": payload["timestamp"],
    }
    assert set(payload) == {"code", "message", "timestamp"}


def test_success_response_shape() -> None:
    assert SuccessResponse().model_dump() == {"success": True}


def test_page_result_omits_empty_pagination_fields() -> None:
    page = PageResult[dict](data=[]).model_dump(mode="json", exclude_none=True)
    assert page == {"object": "list", "data": [], "has_more": False}
    assert "first_id" not in page
    assert "last_id" not in page
    assert "after" not in page
