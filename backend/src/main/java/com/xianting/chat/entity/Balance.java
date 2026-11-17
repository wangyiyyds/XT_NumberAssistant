package com.xianting.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_balance")
public class Balance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long tokenCredits;
    private Boolean autoRefillEnabled;
    private LocalDateTime expireAt;
}
