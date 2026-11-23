"""前端全流程回归（Playwright + 本机 Chrome，前端代码零改动）。

前置：
    1. PostgreSQL 已启动，测试库 xianting_chat_e2e 已初始化（scripts/init_db.py --db ... --create）
    2. mock DeepSeek 在 9001（tests/support/mock_deepseek.py）
    3. 后端在 8080，DATABASE_URL 指向 e2e 库，DEEPSEEK_BASE_URL 指向 mock
    4. 前端 Vite dev server 在 5173（npm run dev）

运行：
    uv run --with playwright python tests/e2e/frontend_flow.py

可通过 BASE_URL / ARTIFACT_DIR 覆盖默认值。脚本只使用 playwright 包和本机已安装的
Chrome（channel="chrome"），不下载浏览器。
"""

from __future__ import annotations

import os
import re
import sys
import time
from pathlib import Path

from playwright.sync_api import Page, expect, sync_playwright

BASE_URL = os.environ.get("BASE_URL", "http://localhost:5173")
ARTIFACT_DIR = Path(
    os.environ.get(
        "ARTIFACT_DIR",
        str(Path(os.environ.get("TEMP", ".")) / "xt-number-assistant-e2e"),
    )
)
ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)

QUESTION = "你好，请用一句话介绍你自己"
MOCK_REPLY_PREFIX = "这是来自 Mock DeepSeek"


def step(message: str) -> None:
    print(f"[e2e] {message}", flush=True)


def shot(page: Page, name: str) -> None:
    path = ARTIFACT_DIR / f"{name}.png"
    page.screenshot(path=str(path), full_page=True)
    step(f"screenshot -> {path}")


def main() -> int:
    console_errors: list[str] = []
    page_errors: list[str] = []

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(channel="chrome", headless=True)
        context = browser.new_context(viewport={"width": 1440, "height": 900}, locale="zh-CN")
        page = context.new_page()
        page.on(
            "console",
            lambda msg: console_errors.append(msg.text) if msg.type == "error" else None,
        )
        page.on("pageerror", lambda error: page_errors.append(str(error)))

        # 1. 登录
        step("打开登录页")
        page.goto(f"{BASE_URL}/login", wait_until="networkidle")
        page.locator('input[type="email"]').fill("test@xianting.cn")
        page.locator('input[type="password"]').fill("123456")
        page.locator('button[type="submit"]').click()
        expect(page).to_have_url(f"{BASE_URL}/", timeout=15_000)
        expect(page.locator(".user-name")).to_have_text("你好，测试用户")
        expect(page.locator(".balance")).to_contain_text("99,980")
        step("登录成功，初始积分 99,980")
        shot(page, "01-login")

        # 2. 智能体中心
        page.locator(".menu-item", has_text="智能体中心").click()
        expect(page).to_have_url(f"{BASE_URL}/agent-center")
        expect(page.locator(".agent-card")).to_have_count(22, timeout=15_000)
        step("智能体中心加载 22 个智能体")
        shot(page, "02-agent-center")

        page.locator(".agent-card", has_text="爆款选题助手").click()
        expect(page).to_have_url(re.compile(r"/chat/"))
        expect(page.locator(".welcome h2")).to_have_text("爆款选题助手")
        step("进入爆款选题助手对话页")

        # 3. 对话流式（mock DeepSeek）
        page.locator("textarea").fill(QUESTION)
        page.keyboard.press("Enter")
        agent_bubble = page.locator(".msg-row.agent .msg-text").last
        expect(agent_bubble).to_contain_text(MOCK_REPLY_PREFIX, timeout=20_000)
        # streaming 结束：发送按钮从「停止」恢复为「发送」
        expect(page.locator("button.send")).to_have_text("发送", timeout=20_000)
        expect(page.locator(".balance")).to_contain_text("99,970")
        step("SSE 流式回复完成，积分 99,970（-10）")
        shot(page, "03-chat-stream")

        # 4. 会话管理：落库、刷新后仍可读取、删除
        expect(page.locator(".conv-item")).to_have_count(2, timeout=10_000)
        page.reload(wait_until="networkidle")
        expect(page.locator(".conv-item")).to_have_count(2, timeout=10_000)
        page.locator(".conv-item").first.click()
        expect(page.locator(".msg-row")).to_have_count(2, timeout=10_000)
        expect(page.locator(".msg-row.user .msg-text").last).to_contain_text(QUESTION)
        step("会话已落库，刷新后可回读 2 条消息")
        shot(page, "04-conversation-history")

        first_conversation = page.locator(".conv-item").first
        first_conversation.hover()
        first_conversation.locator(".conv-del").click()
        expect(page.locator(".conv-item")).to_have_count(1, timeout=10_000)
        page.reload(wait_until="networkidle")
        expect(page.locator(".conv-item")).to_have_count(1, timeout=10_000)
        step("删除会话成功，刷新后仍为 1 条")

        # 5. 灵感中心 + 积分
        page.goto(f"{BASE_URL}/inspiration", wait_until="networkidle")
        expect(page.locator(".keyword-chip")).to_have_count(1, timeout=15_000)
        expect(page.locator(".video-card")).to_have_count(10, timeout=15_000)
        step("灵感中心加载 1 个关键词 / 10 条视频")

        page.get_by_role("button", name=re.compile("点此更新")).click()
        expect(page.locator(".balance")).to_contain_text("99,910", timeout=15_000)
        step("手动刷新扣 60 积分，余额 99,910")
        shot(page, "05-inspiration-refresh")

        page.locator(".add-input").fill("E2E关键词")
        page.get_by_role("button", name="添加").click()
        expect(page.locator(".keyword-chip")).to_have_count(2, timeout=10_000)
        expect(page.locator(".keyword-chip", has_text="E2E关键词")).to_have_count(1)
        step("新增关键词成功")

        chip = page.locator(".keyword-chip", has_text="E2E关键词")
        chip.locator(".chip-close").click()
        expect(page.locator(".keyword-chip")).to_have_count(1, timeout=10_000)
        step("删除关键词成功")

        credits = page.evaluate(
            """async () => {
                const token = localStorage.getItem('xt-auth-token');
                const response = await fetch('/api/balance', {
                    headers: { Authorization: 'Bearer ' + token },
                });
                const body = await response.json();
                return body.data.tokenCredits;
            }"""
        )
        assert credits == 99_910, f"余额应为 99910，实际 {credits}"
        step(f"接口核对积分余额：{credits}")

        # 6. 退出登录
        page.locator(".menu-item", has_text="退出登录").click()
        expect(page).to_have_url(f"{BASE_URL}/login", timeout=10_000)
        assert page.evaluate("localStorage.getItem('xt-auth-token')") is None
        step("退出登录成功，token 已清除")

        browser.close()

    if page_errors:
        print("[e2e] page errors:", *page_errors, sep="\n  ", file=sys.stderr)
    if console_errors:
        print("[e2e] console errors:", *console_errors, sep="\n  ", file=sys.stderr)

    if page_errors:
        return 1
    print(f"[e2e] PASS（截图目录：{ARTIFACT_DIR}）", flush=True)
    return 0


if __name__ == "__main__":
    started = time.time()
    code = main()
    print(f"[e2e] finished in {time.time() - started:.1f}s", flush=True)
    raise SystemExit(code)
