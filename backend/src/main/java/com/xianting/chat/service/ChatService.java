package com.xianting.chat.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/** SSE 流式对话业务接口 */
public interface ChatService {

    /**
     * 发起一次流式对话：同步校验参数与智能体，异步推送
     * conversation / message / final / error 事件。
     */
    void streamChat(SseEmitter emitter, Long userId, Map<String, Object> body);
}
