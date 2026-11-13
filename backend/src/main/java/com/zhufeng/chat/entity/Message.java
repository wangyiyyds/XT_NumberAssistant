package com.zhufeng.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String publicId;
    private Long conversationId;
    private String parentMessageId;
    /** "user" or "agent" */
    private String sender;
    private String text;
    private Boolean error;
    private LocalDateTime createdAt;
}
