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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ExecutorService executor = Executors.newCachedThreadPool();

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
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"message\":\"" + escape(e.getMessage()) + "\"}", MediaType.APPLICATION_JSON));
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
        if (conversationPublicId != null) {
            conversation = conversationRepository.findByPublicIdAndUserId(conversationPublicId, userId);
        }
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

        balanceService.deduct(userId, COST_PER_MESSAGE);

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

        if (isNew) {
            ConversationDto conv = conversationConverter.toDto(conversation);
            conv.setAgentId(agent.getPublicId());
            emitter.send(SseEmitter.event().name("conversation")
                    .data(objectMapper.writeValueAsString(conv), MediaType.APPLICATION_JSON));
        }

        String reply = aiService.generateReply(agent, text);
        String assistantMessageId = UUID.randomUUID().toString();
        for (int i = 0; i < reply.length(); ) {
            int end = Math.min(reply.length(), i + 6);
            String chunk = reply.substring(i, end);
            i = end;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("messageId", assistantMessageId);
            m.put("conversationId", conversation.getPublicId());
            m.put("text", chunk);
            emitter.send(SseEmitter.event().name("message")
                    .data(objectMapper.writeValueAsString(m), MediaType.APPLICATION_JSON));
            Thread.sleep(35);
        }

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

        emitter.send(SseEmitter.event().name("final")
                .data(objectMapper.writeValueAsString(
                        messageConverter.toDto(assistantMsg, conversation.getPublicId())),
                        MediaType.APPLICATION_JSON));
        emitter.complete();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String escape(String s) {
        return s == null ? "internal error" : s.replace("\"", "'");
    }
}
