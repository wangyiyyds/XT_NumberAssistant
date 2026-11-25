package com.xianting.manage.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理端新增用户的请求体：planId 为套餐 publicId，可空表示暂不分配 */
@Data
public class CreateUserRequest {

    /** 对应 t_user.email VARCHAR(128) NOT NULL UNIQUE，入库前 trim + 转小写 */
    @NotBlank(message = "邮箱必填")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128")
    private String email;

    /** 初始密码，至少 6 位（与客户端注册规则一致），落库前加盐哈希 */
    @NotBlank(message = "密码必填")
    @Size(min = 6, max = 64, message = "初始密码至少6位、不超过64位")
    private String password;

    /** 昵称，留空默认「新用户」；对应 t_user.name VARCHAR(64) */
    @Size(max = 64, message = "昵称长度不能超过64")
    private String name;

    /** 套餐 publicId，可空；存在性由 Service 校验（不存在返回 404） */
    @Size(max = 64, message = "套餐标识长度不能超过64")
    private String planId;
}
