"""单元测试：Prompt 组装（人设回退、历史窗口、角色映射）。"""

from __future__ import annotations

from app.models.agent import Agent
from app.models.message import Message
from app.services.ai import (
    DEFAULT_SYSTEM_TEMPLATE,
    HISTORY_WINDOW,
    build_messages,
    build_system_prompt,
)


def make_agent(instructions: str | None) -> Agent:
    return Agent(
        id=1,
        public_id="agent-public",
        department_id=1,
        name="爆款选题助手",
        description=None,
        avatar=None,
        category=None,
        is_preset=True,
        conversation_starters=None,
        instructions=instructions,
    )


def make_message(index: int, sender: str, text: str) -> Message:
    return Message(
        id=index,
        public_id=f"m{index}",
        conversation_id=1,
        parent_message_id=None,
        sender=sender,
        text=text,
        error=False,
    )


def test_system_prompt_uses_agent_instructions() -> None:
    assert build_system_prompt(make_agent("你是一个选题专家")) == "你是一个选题专家"


def test_system_prompt_falls_back_to_name_template() -> None:
    expected = DEFAULT_SYSTEM_TEMPLATE.format(name="爆款选题助手")
    assert build_system_prompt(make_agent(None)) == expected
    assert build_system_prompt(make_agent("   ")) == expected


def test_messages_include_system_history_and_current_user_text() -> None:
    history = [
        make_message(1, "user", "第一问"),
        make_message(2, "agent", "第一答"),
    ]
    messages = build_messages(make_agent("人设"), "第二问", history)
    assert messages == [
        {"role": "system", "content": "人设"},
        {"role": "user", "content": "第一问"},
        {"role": "assistant", "content": "第一答"},
        {"role": "user", "content": "第二问"},
    ]


def test_history_is_limited_to_last_ten_messages() -> None:
    history = [make_message(i, "user" if i % 2 else "agent", f"消息{i}") for i in range(1, 15)]
    messages = build_messages(make_agent(None), "当前问题", history)
    # system + 最近 10 条历史 + 当前用户消息
    assert len(messages) == 1 + HISTORY_WINDOW + 1
    assert messages[1]["content"] == "消息5"
    assert messages[-2]["content"] == "消息14"
    assert messages[-1] == {"role": "user", "content": "当前问题"}


def test_history_without_text_is_skipped() -> None:
    history = [
        make_message(1, "user", "有内容"),
        Message(
            id=2,
            public_id="m2",
            conversation_id=1,
            parent_message_id="m1",
            sender="agent",
            text=None,
            error=True,
        ),
    ]
    messages = build_messages(make_agent(None), "问题", history)
    assert [m["content"] for m in messages[1:]] == ["有内容", "问题"]
