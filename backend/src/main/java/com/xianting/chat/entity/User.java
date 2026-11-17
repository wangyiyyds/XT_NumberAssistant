package com.xianting.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String publicId;
    private String brand;
    private String email;
    private String password;
    private String name;
    private String avatar;
    private Boolean emailVerified;
    private Boolean disabled;
    private LocalDateTime contractReadAt;
    private LocalDateTime createdAt;
}
