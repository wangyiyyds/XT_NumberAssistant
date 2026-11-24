package com.xianting.chat.controller;

import com.xianting.chat.common.UserContext;
import com.xianting.chat.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * SSE streaming chat, shaped after the observed POST /api/agents/chat/agents.
 * Orchestration lives in {@link ChatService}; this layer only wires the request
 * to an emitter.
 */
@RestController
@RequestMapping("/api/agents/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(value = "/agents", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.require();
        // 超时 300 秒。注意这是从请求进来开始算的"总时长"而不是空闲时间：
        // 模型生成总耗时超过它就会切断连接（已生成的部分会由 ChatServiceImpl 以 error=true 落库）。
        // 需与 application.yml 的 spring.mvc.async.request-timeout 保持一致。
        SseEmitter emitter = new SseEmitter(300_000L);
        chatService.streamChat(emitter, userId, body);
        return emitter;
    }
}
