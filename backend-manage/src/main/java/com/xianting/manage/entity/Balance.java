package com.xianting.manage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户积分表（管理端只读，用于用户列表展示余额） */
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
