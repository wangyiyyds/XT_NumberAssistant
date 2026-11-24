package com.xianting.manage.dto;

import lombok.Data;

/** 管理端新增用户的请求体：planId 为套餐 publicId，可空表示暂不分配 */
@Data
public class CreateUserRequest {
    private String email;
    /** 初始密码，至少 6 位，落库前加盐哈希 */
    private String password;
    /** 昵称，留空默认「新用户」 */
    private String name;
    private String planId;
}
