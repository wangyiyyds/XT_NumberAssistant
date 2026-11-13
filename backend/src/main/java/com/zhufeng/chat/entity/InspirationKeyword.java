package com.zhufeng.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_inspiration_keyword")
public class InspirationKeyword {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String keyword;
    private Integer sortOrder;
    private LocalDateTime cacheExpiresAt;
    private LocalDateTime createdAt;
}
