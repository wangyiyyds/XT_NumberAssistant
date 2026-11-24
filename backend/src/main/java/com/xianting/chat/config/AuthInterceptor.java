package com.xianting.chat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xianting.chat.common.Result;
import com.xianting.chat.common.TokenUtil;
import com.xianting.chat.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

@Component
public class AuthInterceptor implements AsyncHandlerInterceptor {

    @Value("${app.jwt.secret}")
    private String secret;

    private final ObjectMapper objectMapper;

    public AuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
        Long userId = TokenUtil.verify(token, secret);
        if (userId == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    objectMapper.writeValueAsString(Result.fail(401, "invalid or expired token")));
            return false;
        }
        UserContext.set(userId);
        return true;
    }

    /**
     * 【异步请求专用】Controller 返回异步结果（如 SseEmitter）、请求进入异步模式后回调。
     *
     * 为什么必须在这里清理：
     * 普通同步请求结束后 Spring 会回调 afterCompletion，我们在那里清 ThreadLocal；
     * 但异步请求（SSE 流式对话）的当前 Tomcat 线程在本方法回调后就直接归还线程池，
     * 请求却还没结束——afterCompletion 不会在这个线程上执行。
     * 如果不在这里清理，userId 会残留在线程上，复用该线程的下一个请求
     * 就可能读到"上一个人"的身份。
     */
    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContext.clear();
    }

    /** 【同步请求】响应写完后清理 ThreadLocal，防止线程复用导致身份串号 */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
