package com.xianting.chat.service.impl;

import com.xianting.chat.entity.Agent;
import com.xianting.chat.entity.Message;
import com.xianting.chat.entity.Prompt;
import com.xianting.chat.repository.PromptRepository;
import com.xianting.chat.service.AiService;
import com.xianting.chat.service.TranscriptCorrectionService;
import com.xianting.chat.service.VideoCopywritingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AiService 默认实现：人设优先取该 Agent 在 t_prompt 的 ACTIVE 版本 content，
 * 缺失时依次回退 Agent.instructions 与通用文案；带最近几条会话历史，流式输出。
 * 上游模型由依赖与配置决定（pom 里的 spring-ai-starter-model-* 与 spring.ai.* 配置），
 * 本类只用 Spring AI 的 ChatClient 抽象，不绑定具体厂商。
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

    /** 带进 prompt 的历史消息条上限，约 5 轮对话 */
    private static final int HISTORY_WINDOW = 10;
    private static final Set<String> VIDEO_COPYWRITING_ASSISTANTS = Set.of("文案提取助手", "文件提取助手");
    private static final Pattern VIDEO_URL = Pattern.compile("https?://[^\\s<>\\\"']+");

    private final ChatClient chatClient;

    private final PromptRepository promptRepository;
    private final VideoCopywritingService videoCopywritingService;
    private final TranscriptCorrectionService transcriptCorrectionService;

    public AiServiceImpl(ChatClient.Builder builder, PromptRepository promptRepository,
                         VideoCopywritingService videoCopywritingService,
                         TranscriptCorrectionService transcriptCorrectionService) {
        this.chatClient = builder.build();
        this.promptRepository = promptRepository;
        this.videoCopywritingService = videoCopywritingService;
        this.transcriptCorrectionService = transcriptCorrectionService;
    }

    @Override
    public Flux<String> streamReply(Agent agent, String userText, List<Message> history) {
        String videoUrl = findVideoUrl(agent, userText);
        if (videoUrl != null) {
            return Flux.defer(() -> Flux.just(transcriptCorrectionService.correct(
                    videoCopywritingService.extract(videoUrl))));
        }

        Prompt active = promptRepository.findActiveByAgentId(agent.getId());
        String system;
        if (active != null && StringUtils.hasText(active.getContent())) {
            system = active.getContent();
            log.debug("agent={} 使用 ACTIVE prompt publicId={} version={}",
                    agent.getPublicId(), active.getPublicId(), active.getVersion());
        } else if (StringUtils.hasText(agent.getInstructions())) {
            // 该智能体没有已发布的提示词版本，回退旧的人设字段
            system = agent.getInstructions();
            log.debug("agent={} 无 ACTIVE prompt，回退 instructions", agent.getPublicId());
        } else {
            system = "你是「" + agent.getName() + "」，请用专业、友好的中文回答用户问题。";
            log.debug("agent={} 无 ACTIVE prompt 且无 instructions，使用通用兜底", agent.getPublicId());
        }
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

    private static String findVideoUrl(Agent agent, String userText) {
        if (!VIDEO_COPYWRITING_ASSISTANTS.contains(agent.getName())) return null;
        Matcher matcher = VIDEO_URL.matcher(userText.replace("\\://", "://"));
        return matcher.find() ? matcher.group() : null;
    }
}
