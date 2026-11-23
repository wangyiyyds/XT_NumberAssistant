"""本地 mock DeepSeek（OpenAI 兼容流式接口），供前端回归使用。

启动：
    uv run python tests/support/mock_deepseek.py

环境变量：
    MOCK_PORT   监听端口，默认 9001
    MOCK_REPLY  完整回复文本

后端只需设置：
    DEEPSEEK_BASE_URL=http://127.0.0.1:9001
    DEEPSEEK_API_KEY=test-key
"""

from __future__ import annotations

import asyncio
import json
import os
import time

import uvicorn
from fastapi import FastAPI
from fastapi.responses import StreamingResponse

app = FastAPI(title="mock-deepseek")

REPLY = os.environ.get(
    "MOCK_REPLY",
    "这是来自 Mock DeepSeek 的流式回复，用于验证前端 SSE 渲染与会话落库。",
)
# 切成 4 段，让前端确实经历增量渲染
PIECES = [
    REPLY[: len(REPLY) // 4],
    REPLY[len(REPLY) // 4 : len(REPLY) // 2],
    REPLY[len(REPLY) // 2 : len(REPLY) * 3 // 4],
    REPLY[len(REPLY) * 3 // 4 :],
]


def chunk(delta: dict, finish_reason: str | None = None) -> str:
    payload = {
        "id": "chatcmpl-mock",
        "object": "chat.completion.chunk",
        "created": int(time.time()),
        "model": "deepseek-chat",
        "choices": [{"index": 0, "delta": delta, "finish_reason": finish_reason}],
    }
    return f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"


@app.post("/chat/completions")
async def chat_completions() -> StreamingResponse:
    async def generate():
        yield chunk({"role": "assistant", "content": ""})
        for piece in PIECES:
            if not piece:
                continue
            yield chunk({"content": piece})
            await asyncio.sleep(0.12)
        yield chunk({}, "stop")
        yield "data: [DONE]\n\n"

    return StreamingResponse(generate(), media_type="text/event-stream")


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


if __name__ == "__main__":
    port = int(os.environ.get("MOCK_PORT", "9001"))
    uvicorn.run(app, host="127.0.0.1", port=port, log_level="warning")
