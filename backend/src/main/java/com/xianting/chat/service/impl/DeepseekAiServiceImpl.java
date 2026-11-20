package com.xianting.chat.service.impl;

import com.xianting.chat.entity.Agent;
import com.xianting.chat.entity.Message;
import com.xianting.chat.service.AiService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;

/** 真实 DeepSeek 上游：人设取 Agent.instructions，带最近几条会话历史，流式输出 */
@Service
public class DeepseekAiServiceImpl implements AiService {

    /** 带进 prompt 的历史消息条上限，约 5 轮对话 */
    private static final int HISTORY_WINDOW = 10;

    private final ChatClient chatClient;

    public DeepseekAiServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public Flux<String> streamReply(Agent agent, String userText, List<Message> history) {
        String system = StringUtils.hasText(agent.getInstructions())
                ? agent.getInstructions()
                : "你是「" + agent.getName() + "」，请用专业、友好的中文回答用户问题。";
        // 实体 Message -> spring-ai 消息类型；按 id 升序取最近 HISTORY_WINDOW 条
        //  处理历史记录
        List<org.springframework.ai.chat.messages.Message> aiHistory = history.stream()
                .sorted(Comparator.comparing(Message::getId))
                .skip(Math.max(0, history.size() - HISTORY_WINDOW))
                .map(m -> "user".equals(m.getSender())
                        ? (org.springframework.ai.chat.messages.Message) new UserMessage(m.getText())
                        : new AssistantMessage(m.getText()))
                .toList();
        return chatClient.prompt()
                .system(system)
                .messages(aiHistory)
                .user(userText)
                .stream()
                .content();
    }
}
