package com.xianting.manage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端登录请求体。原先由 Controller 用 Map&lt;String,String&gt; 收参、
 * Service 手写非空判断，现统一为注解校验。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "username and password are required")
    private String username;

    @NotBlank(message = "username and password are required")
    private String password;
}
