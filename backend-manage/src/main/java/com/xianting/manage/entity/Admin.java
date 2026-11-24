package com.xianting.manage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 管理端管理员账号，独立于客户端 t_user */
@Data
@TableName("t_admin")
public class Admin {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String publicId;
    private String username;
    /** PasswordUtil 格式 saltHex:hashHex */
    private String password;
    private String name;
    private Boolean disabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
