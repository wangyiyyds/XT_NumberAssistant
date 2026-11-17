package com.xianting.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户 DTO，对外字段与原接口一致 */
@Data
public class UserDto {
    /** 对应实体 publicId，绝不暴露数据库自增 id */
    @JsonProperty("_id")
    private String id;
    private String brand;
    private String email;
    private String name;
    private String avatar;
    private Boolean emailVerified;
    private Boolean disabled;
    private LocalDateTime contractReadAt;
    private LocalDateTime createdAt;
}
