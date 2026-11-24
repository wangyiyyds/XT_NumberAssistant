package com.xianting.manage.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 管理端用户列表行 DTO：用户 + 套餐 + 积分三表组装（跨表字段由转换器额外入参提供） */
@Data
public class UserAdminDto {
    /** 对应实体 publicId */
    private String id;
    private String email;
    private String name;
    private String avatar;
    private Boolean emailVerified;
    private Boolean disabled;
    /** 所属套餐 publicId，未分配为 null */
    private String planId;
    private String planName;
    private Long tokenCredits;
    private Boolean autoRefillEnabled;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
}
