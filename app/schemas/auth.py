"""认证相关请求 / 响应体。

Java 版使用 `Map<String, String>` 接收请求，字段缺失时由服务层返回业务错误；
这里同样把 email / password 声明为可选，避免 FastAPI 422 抢先于业务校验。
"""

from pydantic import BaseModel

from app.schemas.user import UserDto


class LoginRequest(BaseModel):
    email: str | None = None
    password: str | None = None
    brand: str | None = None  # 前端会带，登录不校验


class RegisterRequest(BaseModel):
    email: str | None = None
    password: str | None = None
    name: str | None = None
    brand: str | None = None


class AuthResponse(BaseModel):
    token: str
    user: UserDto


class RefreshResponse(BaseModel):
    token: str
