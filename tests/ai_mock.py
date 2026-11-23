"""测试用的 DeepSeek mock。

真实调用会产生网络 / 费用 / 不确定性，SSE 与积分测试统一替换
`AiService.stream_reply`，只保留「逐段产出文本」的接口形状。
"""

from __future__ import annotations

from typing import Any

from app.core.config import settings
from app.services.ai import AiService


def install_fake_ai(
    monkeypatch: Any,
    pieces: tuple[str, ...] = ("你好", "，", "世界"),
) -> list[dict[str, Any]]:
    """把模型替换成固定分片；返回调用记录（供断言 prompt / 历史）。"""

    calls: list[dict[str, Any]] = []

    async def fake_stream_reply(self, agent, user_text, history):
        calls.append(
            {
                "agent": agent.name,
                "user_text": user_text,
                "history": [(m.sender, m.text) for m in history],
            }
        )
        for piece in pieces:
            yield piece

    monkeypatch.setattr(AiService, "stream_reply", fake_stream_reply)
    monkeypatch.setattr(settings, "deepseek_api_key", "test-key")
    return calls


def install_failing_ai(
    monkeypatch: Any,
    message: str = "boom",
    before: str = "部分回复",
) -> None:
    """先产出一段文本再抛错，用于验证 error 事件与落库回滚行为。"""

    async def fake_stream_reply(self, agent, user_text, history):
        if before:
            yield before
        raise RuntimeError(message)

    monkeypatch.setattr(AiService, "stream_reply", fake_stream_reply)
    monkeypatch.setattr(settings, "deepseek_api_key", "test-key")
