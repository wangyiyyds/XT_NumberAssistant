"""AI 流式回复（对应 Java `DeepseekAiServiceImpl`）。

用 openai SDK 调 DeepSeek 兼容接口，逐段产出文本。人设取
`agent.instructions`，为空时回退到「你是「名称」，请用专业、友好的中文回答用户问题。」；
带最近 10 条同会话历史（约 5 轮对话）。

`DEEPSEEK_API_KEY` 缺失不阻止应用启动，只在真正调用时抛错。
"""

from __future__ import annotations

import logging
from collections.abc import AsyncIterator, Sequence

from openai import AsyncOpenAI

from app.core.config import settings
from app.core.exceptions import ApiException
from app.models.agent import Agent
from app.models.message import Message

logger = logging.getLogger("app.ai")

# 带进 prompt 的历史消息条上限，约 5 轮对话（与 Java 版一致）
HISTORY_WINDOW = 10

# 与 Java `SseEmitter(120_000L)` / spring.mvc.async.request-timeout 对齐
REQUEST_TIMEOUT_SECONDS = 120.0

DEFAULT_SYSTEM_TEMPLATE = "你是「{name}」，请用专业、友好的中文回答用户问题。"


def build_system_prompt(agent: Agent) -> str:
    """人设取 instructions；空白时按名称生成默认提示。"""

    if agent.instructions is not None and agent.instructions.strip():
        return agent.instructions
    return DEFAULT_SYSTEM_TEMPLATE.format(name=agent.name)


def build_messages(
    agent: Agent, user_text: str, history: Sequence[Message]
) -> list[dict[str, str]]:
    """组装 DeepSeek messages：system + 最近 10 条历史 + 本次用户消息。"""

    messages: list[dict[str, str]] = [{"role": "system", "content": build_system_prompt(agent)}]
    # history 已按 id 升序；Java 版等价于按 id 排序后取最后 HISTORY_WINDOW 条
    for message in list(history)[-HISTORY_WINDOW:]:
        if message.text is None:
            continue
        role = "user" if message.sender == "user" else "assistant"
        messages.append({"role": role, "content": message.text})
    messages.append({"role": "user", "content": user_text})
    return messages


class AiService:
    """DeepSeek 流式对话。"""

    async def stream_reply(
        self, agent: Agent, user_text: str, history: Sequence[Message]
    ) -> AsyncIterator[str]:
        """逐段产出模型回复。"""

        api_key = settings.deepseek_api_key.strip()
        if not api_key:
            raise ApiException(500, "DEEPSEEK_API_KEY 未配置，无法调用大模型")

        messages = build_messages(agent, user_text, history)

        # 每次调用独立客户端：流结束（或客户端断开）即释放连接
        async with AsyncOpenAI(
            api_key=api_key,
            base_url=settings.deepseek_base_url,
            timeout=REQUEST_TIMEOUT_SECONDS,
        ) as client:
            stream = await client.chat.completions.create(
                model=settings.deepseek_model,
                temperature=settings.deepseek_temperature,
                messages=messages,  # type: ignore[arg-type]
                stream=True,
            )
            try:
                async for chunk in stream:
                    if not chunk.choices:
                        continue
                    piece = chunk.choices[0].delta.content
                    if piece:
                        yield piece
            finally:
                # 上游被取消 / 客户端断开时立即释放 HTTP 连接
                await stream.close()
