package com.xianting.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 余额 DTO，对外字段与原接口一致 */
@Data
public class BalanceDto {
    private Long tokenCredits;
    private boolean autoRefillEnabled;
    private LocalDateTime expireAt;
    /** 用户 publicId */
    private String user;
}
