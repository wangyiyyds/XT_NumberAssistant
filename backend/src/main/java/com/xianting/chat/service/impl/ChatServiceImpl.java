package com.xianting.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xianting.chat.common.ApiException;
import com.xianting.chat.converter.ConversationConverter;
import com.xianting.chat.converter.MessageConverter;
import com.xianting.chat.dto.ConversationDto;
import com.xianting.chat.entity.Agent;
import com.xianting.chat.entity.Conversation;
import com.xianting.chat.entity.Message;
import com.xianting.chat.repository.ConversationRepository;
import com.xianting.chat.repository.MessageRepository;
import com.xianting.chat.service.AgentService;
import com.xianting.chat.service.AiService;
import com.xianting.chat.service.BalanceService;
import com.xianting.chat.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * SSE 流式对话：事件 "conversation"（新会话）、"message"（文本块）、
 * "final"（完整回复）、"error"。
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final long COST_PER_MESSAGE = 10;

    private final AgentService agentService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final BalanceService balanceService;
    private final AiService aiService;
    private final ConversationConverter conversationConverter;
    private final MessageConverter messageConverter;
    private final ObjectMapper objectMapper;
    // 守护线程：JVM 关闭时不会被卡住的流式请求拖住不退，避免端口残留占用
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    public ChatServiceImpl(AgentService agentService,
                           ConversationRepository conversationRepository,
                           MessageRepository messageRepository,
                           BalanceService balanceService,
                           AiService aiService,
                           ConversationConverter conversationConverter,
                           MessageConverter messageConverter,
                           ObjectMapper objectMapper) {
        this.agentService = agentService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.balanceService = balanceService;
        this.aiService = aiService;
        this.conversationConverter = conversationConverter;
        this.messageConverter = messageConverter;
        this.objectMapper = objectMapper;
    }

    @Override
    public void streamChat(SseEmitter emitter, Long userId, Map<String, Object> body) {
        String agentPublicId = str(body.get("agent_id"));
        String text = str(body.get("text"));
        String conversationPublicId = str(body.get("conversationId"));
        String clientMessageId = str(body.get("messageId"));
        String parentMessageId = str(body.get("parentMessageId"));
        if (agentPublicId == null || text == null || text.isBlank()) {
            throw new ApiException(400, "agent_id and text are required");
        }
        Agent agent = agentService.requireByPublicId(agentPublicId);

        executor.submit(() -> {
            try {
                runChat(emitter, userId, agent, text, conversationPublicId, clientMessageId, parentMessageId);
            } catch (Exception e) {
                try {
                    // 用 objectMapper 序列化，模型异常文本里的引号/换行不会拼出非法 JSON
                    emitter.send(SseEmitter.event().name("error")
                            .data(objectMapper.writeValueAsString(Map.of(
                                    "message", e.getMessage() == null ? "internal error" : e.getMessage())),
                                    MediaType.APPLICATION_JSON));
                } catch (Exception ignored) {
                }
                emitter.complete();
            }
        });
    }

    private void runChat(SseEmitter emitter, Long userId, Agent agent, String text,
                         String conversationPublicId, String clientMessageId, String parentMessageId) throws Exception {
        Conversation conversation = null;
        boolean isNew = false;
        //如果有历史会话就读取
        if (conversationPublicId != null) {
            conversation = conversationRepository.findByPublicIdAndUserId(conversationPublicId, userId);
        }
        //没有历史会话则新建会话
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setPublicId(UUID.randomUUID().toString());
            conversation.setUserId(userId);
            conversation.setAgentId(agent.getId());
            conversation.setTitle(text.length() > 20 ? text.substring(0, 20) : text);
            conversation.setEndpoint("agents");
            conversation.setCreatedAt(LocalDateTime.now());
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conversation);
            isNew = true;
        }
        //当前用户系统余额是否充足
        balanceService.deduct(userId, COST_PER_MESSAGE);

        //构建用户消息对象
        String userMessageId = clientMessageId != null ? clientMessageId : UUID.randomUUID().toString();
        Message userMsg = new Message();
        userMsg.setPublicId(userMessageId);
        userMsg.setConversationId(conversation.getId());
        userMsg.setParentMessageId(parentMessageId);
        userMsg.setSender("user");
        userMsg.setText(text);
        userMsg.setError(false);
        userMsg.setCreatedAt(LocalDateTime.now());
        messageRepository.save(userMsg);

        //新会话 需要和前端建立sse连接
        if (isNew) {
            ConversationDto conv = conversationConverter.toDto(conversation);
            conv.setAgentId(agent.getPublicId());
            emitter.send(SseEmitter.event().name("conversation")
                    .data(objectMapper.writeValueAsString(conv), MediaType.APPLICATION_JSON));
        }

        // 同会话历史（本次用户消息已先落库，按 publicId 排除）
        List<Message> history = messageRepository.listByConversationId(conversation.getId()).stream()
                .filter(m -> !m.getPublicId().equals(userMessageId))
                .toList();

        String assistantMessageId = UUID.randomUUID().toString();
        StringBuilder full = new StringBuilder();
        // 真流式：在守护 worker 线程上阻塞消费模型片段，逐段转发 message 事件；
        // 客户端断开时 send 抛异常 -> try-with-resources 关流并取消上游订阅
        try (Stream<String> chunks = aiService.streamReply(agent, text, history).toStream()) {
            var it = chunks.iterator();
            while (it.hasNext()) {
                String chunk = it.next();
                if (chunk.isEmpty()) {
                    continue;
                }
                full.append(chunk);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("messageId", assistantMessageId);
                m.put("conversationId", conversation.getPublicId());
                m.put("text", chunk);
                //把每一个chunk 通过emitter的message事件发送给VUE前端
                emitter.send(SseEmitter.event().name("message")
                        .data(objectMapper.writeValueAsString(m), MediaType.APPLICATION_JSON));
            }
        }
        String reply = full.toString();

        //把AI响应的消息存起来 会话历史存储
        Message assistantMsg = new Message();
        assistantMsg.setPublicId(assistantMessageId);
        assistantMsg.setConversationId(conversation.getId());
        assistantMsg.setParentMessageId(userMessageId);
        assistantMsg.setSender("agent");
        assistantMsg.setText(reply);
        assistantMsg.setError(false);
        assistantMsg.setCreatedAt(LocalDateTime.now());
        messageRepository.save(assistantMsg);

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.updateById(conversation);

        //本次流式响应结束 final连接可以断开
        emitter.send(SseEmitter.event().name("final")
                .data(objectMapper.writeValueAsString(
                        messageConverter.toDto(assistantMsg, conversation.getPublicId())),
                        MediaType.APPLICATION_JSON));
        emitter.complete();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
