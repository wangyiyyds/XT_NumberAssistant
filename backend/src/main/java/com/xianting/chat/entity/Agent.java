package com.xianting.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_agent")
public class Agent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String publicId;
    private Long departmentId;
    private String name;
    private String description;
    private String avatar;
    private String category;
    private Boolean isPreset;
    /** JSON array string, e.g. ["starter1","starter2"] */
    private String conversationStarters;
    private String instructions;
    private LocalDateTime createdAt;
}
