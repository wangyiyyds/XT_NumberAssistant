package com.xianting.chat.service;

import com.xianting.chat.entity.Agent;
import com.xianting.chat.entity.Message;
import reactor.core.publisher.Flux;

import java.util.List;

/** AI 回复生成接口：流式上游，便于替换不同大模型实现 */
public interface AiService {

    /**
     * 流式生成回复，逐段返回文本。
     *
     * @param agent    智能体（其 ACTIVE 版提示词 t_prompt.content 作为 system 人设，无 ACTIVE 版时回退 instructions / 兜底文案）
     * @param userText 本次用户消息
     * @param history  同会话历史消息（id 升序，不含本次用户消息；实现内部截取最近几条）
     */
    Flux<String> streamReply(Agent agent, String userText, List<Message> history);
}
