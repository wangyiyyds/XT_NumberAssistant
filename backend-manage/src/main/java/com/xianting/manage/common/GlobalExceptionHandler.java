package com.xianting.manage.common;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
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
