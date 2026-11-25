package com.xianting.manage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员新建请求体（密码只进不出，任何响应不回显） */
@Data
public class AdminSaveRequest {

    /** 对应 t_admin.username VARCHAR(64) NOT NULL UNIQUE */
    @NotBlank(message = "登录账号必填")
    @Size(max = 64, message = "登录账号长度不能超过64")
    private String username;

    /** 明文密码，Service 层经 PasswordUtil.hash 后入库；列宽存的是 saltHex:hashHex(129)，与明文长度无关 */
    @NotBlank(message = "密码必填")
    @Size(min = 6, max = 64, message = "密码至少6位、不超过64位")
    private String password;

    /** 对应 t_admin.name VARCHAR(64) */
    @Size(max = 64, message = "姓名长度不能超过64")
    private String name;
}
