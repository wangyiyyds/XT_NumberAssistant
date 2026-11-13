package com.zhufeng.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhufeng.chat.common.ApiException;
import com.zhufeng.chat.common.UserContext;
import com.zhufeng.chat.entity.*;
import com.zhufeng.chat.mapper.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SSE streaming chat, shaped after the observed POST /api/agents/chat/agents.
 * Events: "conversation" (new conversation), "message" (text chunk), "final" (full assistant message), "error".
 */
@RestController
@RequestMapping("/api/agents/chat")
public class ChatController {

    private static final long COST_PER_MESSAGE = 10;

    private final AgentMapper agentMapper;
    private final AgentController agentController;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final BalanceMapper balanceMapper;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ChatController(AgentMapper agentMapper, AgentController agentController,
                          ConversationMapper conversationMapper, MessageMapper messageMapper,
                          BalanceMapper balanceMapper, ObjectMapper objectMapper) {
        this.agentMapper = agentMapper;
        this.agentController = agentController;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.balanceMapper = balanceMapper;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/agents", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.require();
        String agentPublicId = str(body.get("agent_id"));
        String text = str(body.get("text"));
        String conversationPublicId = str(body.get("conversationId"));
        String clientMessageId = str(body.get("messageId"));
        String parentMessageId = str(body.get("parentMessageId"));
        if (agentPublicId == null || text == null || text.isBlank()) {
            throw new ApiException(400, "agent_id and text are required");
        }
        Agent agent = agentController.findByPublicId(agentPublicId);

        SseEmitter emitter = new SseEmitter(120_000L);
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
        return emitter;
    }

    private void runChat(SseEmitter emitter, Long userId, Agent agent, String text,
                         String conversationPublicId, String clientMessageId, String parentMessageId) throws Exception {
        Conversation conversation = null;
        boolean isNew = false;
        if (conversationPublicId != null) {
            QueryWrapper<Conversation> qw = new QueryWrapper<>();
            qw.eq("public_id", conversationPublicId).eq("user_id", userId);
            conversation = conversationMapper.selectOne(qw);
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
            conversationMapper.insert(conversation);
            isNew = true;
        }

        deductBalance(userId);

        String userMessageId = clientMessageId != null ? clientMessageId : UUID.randomUUID().toString();
        Message userMsg = new Message();
        userMsg.setPublicId(userMessageId);
        userMsg.setConversationId(conversation.getId());
        userMsg.setParentMessageId(parentMessageId);
        userMsg.setSender("user");
        userMsg.setText(text);
        userMsg.setError(false);
        userMsg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(userMsg);

        if (isNew) {
            Map<String, Object> conv = ConversationController.toVo(conversation);
            conv.put("agent_id", agent.getPublicId());
            emitter.send(SseEmitter.event().name("conversation")
                    .data(objectMapper.writeValueAsString(conv), MediaType.APPLICATION_JSON));
        }

        String reply = MockAi.generateReply(agent, text);
        String assistantMessageId = UUID.randomUUID().toString();
        StringBuilder sent = new StringBuilder();
        for (int i = 0; i < reply.length(); ) {
            int end = Math.min(reply.length(), i + 6);
            String chunk = reply.substring(i, end);
            i = end;
            sent.append(chunk);
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
        messageMapper.insert(assistantMsg);

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);

        emitter.send(SseEmitter.event().name("final")
                .data(objectMapper.writeValueAsString(
                        ConversationController.toVo(assistantMsg, conversation.getPublicId())),
                        MediaType.APPLICATION_JSON));
        emitter.complete();
    }

    private void deductBalance(Long userId) {
        QueryWrapper<Balance> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        Balance balance = balanceMapper.selectOne(qw);
        if (balance == null) return;
        long credits = balance.getTokenCredits() == null ? 0 : balance.getTokenCredits();
        balance.setTokenCredits(Math.max(0, credits - COST_PER_MESSAGE));
        balanceMapper.updateById(balance);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String escape(String s) {
        return s == null ? "internal error" : s.replace("\"", "'");
    }
}
