"""单元测试：token 签发 / 校验、密码哈希。

Token / 密码格式必须与 Java 版逐字节一致，否则旧 token、旧账号无法继续使用。
"""

from __future__ import annotations

import base64
import hashlib
import hmac
import re

import pytest

from app.core import security
from app.core.security import hash_password, issue_token, verify_password, verify_token

SECRET = "xianting-chat-clone-secret-key-change-in-production"


def _b64(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def _java_token(payload: str, secret: str) -> str:
    """独立复刻 Java `TokenUtil.issue` 的拼装过程（不调用被测实现）。"""

    signature = hmac.new(secret.encode(), payload.encode(), hashlib.sha256).digest()
    return f"{_b64(payload.encode())}.{_b64(signature)}"


# ---------------------------------------------------------------------------
# Token
# ---------------------------------------------------------------------------


def test_issue_token_matches_java_byte_format(monkeypatch: pytest.MonkeyPatch) -> None:
    """固定时钟后，token 与 Java 算法输出完全一致。"""

    monkeypatch.setattr(security.time, "time", lambda: 1_700_000_000.0)
    token = issue_token(42, SECRET, 2)
    # exp = 1_700_000_000_000 + 2 * 3_600_000
    assert token == _java_token("42:1700007200000", SECRET)


def test_verify_accepts_java_style_token() -> None:
    token = _java_token("1:4102444800000", SECRET)  # 2100-01-01
    assert verify_token(token, SECRET) == 1


def test_token_roundtrip() -> None:
    token = issue_token(7, SECRET, 1)
    assert verify_token(token, SECRET) == 7


def test_token_is_url_safe_and_unpadded() -> None:
    token = issue_token(123, SECRET, 1)
    payload, signature = token.split(".")
    assert "=" not in token
    assert re.fullmatch(r"[A-Za-z0-9_-]+", payload)
    assert re.fullmatch(r"[A-Za-z0-9_-]+", signature)


def test_expired_token_is_rejected() -> None:
    assert verify_token(issue_token(1, SECRET, -1), SECRET) is None


def test_wrong_secret_is_rejected() -> None:
    token = issue_token(1, SECRET, 1)
    assert verify_token(token, "another-secret") is None


def test_tampered_payload_is_rejected() -> None:
    valid = issue_token(1, SECRET, 1)
    _, signature = valid.split(".")
    forged = _b64(b"2:4102444800000")  # 改成用户 2，但沿用用户 1 的签名
    assert verify_token(f"{forged}.{signature}", SECRET) is None


def test_malformed_tokens_are_rejected() -> None:
    for token in [None, "", "abc", "a.b.c", "!!!.???", "MQ.MQ", "x.y"]:
        assert verify_token(token, SECRET) is None


def test_non_numeric_payload_is_rejected() -> None:
    token = _java_token("user:4102444800000", SECRET)
    assert verify_token(token, SECRET) is None


# ---------------------------------------------------------------------------
# 密码
# ---------------------------------------------------------------------------


SEED_PASSWORD_HASH = (
    "f854ea4702824102b9c6cc09a6ad0be9:"
    "e245c309c15fdc30f62a2b4f5a71bb3dc3223f73279b6f4d6800ff7cc00d8331"
)


def test_password_hash_format_matches_java() -> None:
    stored = hash_password("123456")
    assert re.fullmatch(r"[0-9a-f]{32}:[0-9a-f]{64}", stored)


def test_password_hash_roundtrip() -> None:
    stored = hash_password("s3cret!")
    assert verify_password("s3cret!", stored) is True
    assert verify_password("wrong", stored) is False


def test_password_hash_uses_random_salt() -> None:
    assert hash_password("123456") != hash_password("123456")


def test_seed_password_matches_java_hash() -> None:
    """db/data.sql 里的 Java 哈希必须能被 Python 校验通过。"""

    assert verify_password("123456", SEED_PASSWORD_HASH) is True
    assert verify_password("654321", SEED_PASSWORD_HASH) is False


def test_invalid_stored_password_is_rejected() -> None:
    for stored in [None, "", "no-colon", "salt:", ":" * 10]:
        assert verify_password("123456", stored) is False


def test_java_password_algorithm_is_reproduced() -> None:
    """已知 salt 下，摘要与 Java `PasswordUtil.hash` 输出一致。"""

    salt = "0123456789abcdef0123456789abcdef"
    expected = hashlib.sha256((salt + "123456").encode()).hexdigest()
    stored = f"{salt}:{expected}"
    assert verify_password("123456", stored) is True
