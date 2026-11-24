package com.xianting.manage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xianting.manage.common.AdminContext;
import com.xianting.manage.common.Result;
import com.xianting.manage.common.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理端登录拦截器：校验 Bearer token（载荷是 adminId）并写入 AdminContext。
 * 管理端没有 SSE 异步接口，用同步 HandlerInterceptor 即可，
 * afterCompletion 清 ThreadLocal 防止线程复用串号。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

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
        Long adminId = TokenUtil.verify(token, secret);
        if (adminId == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    objectMapper.writeValueAsString(Result.fail(401, "invalid or expired token")));
            return false;
        }
        AdminContext.set(adminId);
        return true;
    }

    /** 响应写完后清理 ThreadLocal，防止线程复用导致身份串号 */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminContext.clear();
    }
}
