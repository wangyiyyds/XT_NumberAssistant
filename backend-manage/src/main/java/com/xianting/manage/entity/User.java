package com.xianting.manage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户端用户表的管理端映射：比客户端 User 实体多一个 planId（所属套餐）。
 * planId 可空；客户端实体不映射该列，两端互不影响。
 */
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
    /** 所属套餐，FK 逻辑关联 -> t_plan.id，NULL 表示未分配 */
    private Long planId;
    private LocalDateTime contractReadAt;
    private LocalDateTime createdAt;
}
