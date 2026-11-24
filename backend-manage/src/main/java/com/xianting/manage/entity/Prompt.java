package com.xianting.manage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体提示词的版本记录，一个智能体可有多个版本，仅一个处于 ACTIVE。
 * 状态机：DRAFT -> ACTIVE -> ARCHIVED（管理端发布时旧 ACTIVE 转 ARCHIVED）。
 * 表里的 active_agent_id 是数据库生成列（status='ACTIVE' 时取 agent_id），
 * 只用于让唯一索引 uk_agent_active 约束「每个智能体至多一条 ACTIVE」，
 * 不可写入，故不在本实体中映射。
 */
@Data
@TableName("t_prompt")
public class Prompt {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String publicId;
    /** 归属智能体，FK -> t_agent.id（ON DELETE CASCADE） */
    private Long agentId;
    private Integer version;
    private String versionTag;
    private String name;
    private String description;
    /** 提示词用途，目前只有 SYSTEM */
    private String promptType;
    /** 交给大模型的正文 */
    private String content;
    /** JSON array string, e.g. [{"name":"platform","default":"抖音"}] */
    private String variables;
    /** JSON string, e.g. {"model":"deepseek-chat","temperature":0.9} */
    private String modelConfig;
    /** DRAFT / ACTIVE / ARCHIVED */
    private String status;
    /** 血缘：从哪个版本改出来的 */
    private Long parentPromptId;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime archivedAt;
}
