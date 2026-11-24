package com.xianting.chat.service.impl;

import com.xianting.chat.entity.Agent;
import com.xianting.chat.entity.Message;
import com.xianting.chat.service.AiService;
import com.xianting.chat.service.TranscriptCorrectionService;
import com.xianting.chat.service.VideoCopywritingService;
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

/** 真实 DeepSeek 上游：人设取 Agent.instructions，带最近几条会话历史，流式输出 */
@Service
public class DeepseekAiServiceImpl implements AiService {

    /** 带进 prompt 的历史消息条上限，约 5 轮对话 */
    private static final int HISTORY_WINDOW = 10;
    private static final Set<String> VIDEO_COPYWRITING_ASSISTANTS = Set.of("文案提取助手", "文件提取助手");
    private static final Pattern VIDEO_URL = Pattern.compile("https?://[^\\s<>\\\"']+");

    private final ChatClient chatClient;
    private final VideoCopywritingService videoCopywritingService;
    private final TranscriptCorrectionService transcriptCorrectionService;

    public DeepseekAiServiceImpl(ChatClient.Builder builder,
                                 VideoCopywritingService videoCopywritingService,
                                 TranscriptCorrectionService transcriptCorrectionService) {
        this.chatClient = builder.build();
        this.videoCopywritingService = videoCopywritingService;
        this.transcriptCorrectionService = transcriptCorrectionService;
    }

    @Override
    public Flux<String> streamReply(Agent agent, String userText, List<Message> history) {
        String videoUrl = findVideoUrl(agent, userText);
        if (videoUrl != null) {
            // 第 1 段：本地小模型听写；第 2 段：DeepSeek 只校正文本，不接收音视频。
            return Flux.defer(() -> Flux.just(transcriptCorrectionService.correct(
                    videoCopywritingService.extract(videoUrl))));
        }
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

    private static String findVideoUrl(Agent agent, String userText) {
        if (!VIDEO_COPYWRITING_ASSISTANTS.contains(agent.getName())) return null;
        Matcher matcher = VIDEO_URL.matcher(userText.replace("\\://", "://"));
        return matcher.find() ? matcher.group() : null;
    }
}
