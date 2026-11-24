package com.xianting.manage.dto;

import lombok.Data;

/** 管理员新建请求体（密码只进不出，任何响应不回显） */
@Data
public class AdminSaveRequest {
    private String username;
    /** 明文密码，Service 层经 PasswordUtil.hash 后入库 */
    private String password;
    private String name;
}
