package com.xianting.manage.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 管理员 DTO，password 不出现在任何 DTO 中 */
@Data
public class AdminDto {
    /** 对应实体 publicId */
    private String id;
    private String username;
    private String name;
    private LocalDateTime createdAt;
}
