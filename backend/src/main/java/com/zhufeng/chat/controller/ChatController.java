package com.zhufeng.chat.controller;

import com.zhufeng.chat.common.UserContext;
import com.zhufeng.chat.service.ChatService;
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
        SseEmitter emitter = new SseEmitter(120_000L);
        chatService.streamChat(emitter, userId, body);
        return emitter;
    }
}
