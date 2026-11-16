package com.zhufeng.chat.service.impl;

import com.zhufeng.chat.entity.Agent;
import com.zhufeng.chat.service.AiService;
import org.springframework.stereotype.Service;

/** 确定性 mock 回复，替代真实大模型上游。 */
@Service
public class MockAiServiceImpl implements AiService {

    @Override
    public String generateReply(Agent agent, String userText) {
        String topic = userText.length() > 30 ? userText.substring(0, 30) + "…" : userText;
        return "你好！我是" + agent.getName() + "，隶属于" + agent.getCategory() + "。\n\n"
                + "关于「" + topic + "」，我的初步分析如下：\n\n"
                + "1. **现状梳理**：这是一个值得深入探讨的方向，建议先明确目标受众和核心卖点。\n"
                + "2. **执行建议**：可以按「调研 → 方案 → 小步验证 → 复盘迭代」四步推进，每步产出可量化的结果。\n"
                + "3. **风险提示**：注意资源投入与节奏把控，优先做低成本、高反馈的尝试。\n\n"
                + "如果你能补充更多背景（比如预算、时间线、已有素材），我可以给出更具体的方案。";
    }
}
