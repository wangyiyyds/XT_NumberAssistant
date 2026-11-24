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

        // 同步扣积分（余额不足抛 402）：必须放在返回 SseEmitter 之前，
        // 异常才能经 GlobalExceptionHandler 变成正常的 HTTP 402 JSON 响应
        // （前端 sse.ts 里 response.ok=false 会直接 throw 给 catch 块）；
        // 如果放到异步线程里扣，HTTP 状态已经是 200，只能用 error 事件通知，前端难以区分。
        balanceService.deductOrThrow(userId, COST_PER_MESSAGE);

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
        // 积分已在 streamChat 的同步阶段扣过（余额不足的请求根本进不到这里）

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

        // 同会话历史（本次用户消息已先落库，按 publicId 排除；
        // error=true 的历史回复是失败留下的半截内容，不喂给模型，避免污染上下文）
        List<Message> history = messageRepository.listByConversationId(conversation.getId()).stream()
                .filter(m -> !m.getPublicId().equals(userMessageId))
                .filter(m -> !Boolean.TRUE.equals(m.getError()))
                .toList();

        String assistantMessageId = UUID.randomUUID().toString();
        StringBuilder full = new StringBuilder();
        // assistantMsg 在 try 之前创建、finally 里统一落库：
        // 不管流式生成成功还是中途失败，这条回复在库里都有记录
        Message assistantMsg = new Message();
        boolean failed = false;
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
        } catch (Exception e) {
            // 模型上游报错、客户端断开导致 SSE 发送失败、超时切断等都会走到这里
            failed = true;
            // 原样往上抛：由 streamChat 外层的 catch 给前端发 error 事件
            throw e;
        } finally {
            //【关键】无论成功失败都把回复落库：
            // 失败时 error=true，full 里是断掉前已生成的半截内容。
            // 这样重新打开会话，前端能把这条半截回复按出错样式渲染出来，
            // 而不是旧行为那样"提问在、回答没了、积分也扣了"。
            assistantMsg.setPublicId(assistantMessageId);
            assistantMsg.setConversationId(conversation.getId());
            assistantMsg.setParentMessageId(userMessageId);
            assistantMsg.setSender("agent");
            assistantMsg.setText(full.toString());
            assistantMsg.setError(failed);
            assistantMsg.setCreatedAt(LocalDateTime.now());
            messageRepository.save(assistantMsg);

            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.updateById(conversation);
        }

        // 只有 try 正常走完（没抛异常）才会执行到这里：发 final 事件告诉前端本轮结束，连接可以断开
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
