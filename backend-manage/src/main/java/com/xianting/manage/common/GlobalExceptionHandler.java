package com.xianting.manage.common;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Result<Void>> handleApi(ApiException e) {
        return ResponseEntity.status(e.getStatus())
                .body(Result.fail(e.getStatus(), e.getMessage()));
    }

    /**
     * @Valid 校验请求体失败：取第一条字段错误的 message 返回 400。
     * DTO 上的 message 已写成原手写校验的中文文案，前端展示不变。
     * 必须显式处理：否则该异常会被下面的 Exception.class 兜底成 500 并泄露内部信息。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleInvalidBody(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("请求参数不合法");
        return ResponseEntity.status(400)
                .body(Result.fail(400, message));
    }

    /**
     * 唯一索引冲突统一转 409：
     * - uk_agent_active：并发发布提示词时后提交者触发
     * - uk_agent_version：并发建草稿版本撞号
     * - uk_plan_level / t_admin.username 等业务唯一约束
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKey(DuplicateKeyException e) {
        return ResponseEntity.status(409)
                .body(Result.fail(409, "数据冲突，请刷新后重试"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        return ResponseEntity.status(500)
                .body(Result.fail(500, e.getMessage() == null ? "internal error" : e.getMessage()));
    }
}
