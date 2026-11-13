package com.zhufeng.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_conversation")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String publicId;
    private Long userId;
    private Long agentId;
    private String title;
    private String endpoint;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
