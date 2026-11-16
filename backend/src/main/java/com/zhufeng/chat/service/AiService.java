package com.zhufeng.chat.service;

import com.zhufeng.chat.entity.Agent;

/** AI 回复生成接口：便于后续替换为真实大模型上游 */
public interface AiService {

    String generateReply(Agent agent, String userText);
}
