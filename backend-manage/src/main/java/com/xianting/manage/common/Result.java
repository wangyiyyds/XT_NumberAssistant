package com.xianting.manage.common;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一响应外壳：所有 REST JSON 接口返回 {code, message, data, timestamp}。
 * HTTP 状态码保持真实语义（如 401/404/409），code 字段与之一致。
 */
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private String timestamp;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now().toString();
    }

    /** 成功：原业务返回值原样放进 data（旧信封不拆） */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /** 失败：data 为 null，配合真实 HTTP 状态码返回 */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
