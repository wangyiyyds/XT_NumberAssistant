"""Token 与密码工具，逐字节复刻 Java 版，保证旧 token / 账号继续可用。

Token 格式（Java `common/TokenUtil`）::

    base64url(payload).base64url(HmacSHA256(payload))
    payload = userId:expiresAtMillis
    base64url 不带 padding

密码格式（Java `common/PasswordUtil`）::

    hex(salt):sha256(hex(salt) + rawPassword)
"""

import base64
import hashlib
import hmac
import secrets
import time


def _b64_encode(data: bytes) -> str:
    """Java `Base64.getUrlEncoder().withoutPadding()` 等价实现。"""

    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def _b64_decode(value: str) -> bytes:
    """Java `Base64.getUrlDecoder()` 等价实现（自动补齐 padding）。"""

    padding = "=" * (-len(value) % 4)
    return base64.b64decode(value + padding, altchars=b"-_", validate=True)


def _sign(payload: str, secret: str) -> bytes:
    return hmac.new(secret.encode("utf-8"), payload.encode("utf-8"), hashlib.sha256).digest()


def issue_token(user_id: int, secret: str, expire_hours: int) -> str:
    """签发 token（毫秒过期时间）。"""

    expires_at = int(time.time() * 1000) + expire_hours * 3_600_000
    payload = f"{user_id}:{expires_at}"
    return f"{_b64_encode(payload.encode('utf-8'))}.{_b64_encode(_sign(payload, secret))}"


def verify_token(token: str | None, secret: str) -> int | None:
    """校验 token，返回 userId；无效 / 过期返回 None（与 Java 一致）。"""

    if token is None:
        return None
    parts = token.split(".")
    if len(parts) != 2:
        return None
    try:
        payload = _b64_decode(parts[0]).decode("utf-8")
        signature = _b64_decode(parts[1])
        if not hmac.compare_digest(signature, _sign(payload, secret)):
            return None
        fields = payload.split(":")
        if len(fields) != 2:
            return None
        expires_at = int(fields[1])
        if expires_at < int(time.time() * 1000):
            return None
        return int(fields[0])
    except (ValueError, UnicodeDecodeError):
        return None


def hash_password(raw_password: str) -> str:
    """生成 `hex(salt):sha256(salt + password)`。"""

    salt = secrets.token_bytes(16).hex()
    digest = hashlib.sha256((salt + raw_password).encode("utf-8")).hexdigest()
    return f"{salt}:{digest}"


def verify_password(raw_password: str, stored: str | None) -> bool:
    """校验密码；格式不符直接返回 False。"""

    if stored is None or ":" not in stored:
        return False
    salt, _, digest = stored.partition(":")
    expected = hashlib.sha256((salt + raw_password).encode("utf-8")).hexdigest()
    return hmac.compare_digest(expected, digest)
