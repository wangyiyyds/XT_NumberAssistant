package com.zhufeng.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/** 会话 DTO，对外字段与原接口一致 */
@Data
public class ConversationDto {
    /** 对应实体 publicId */
    @JsonProperty("_id")
    private String id;
    private String conversationId;
    private String title;
    private String endpoint;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 仅 SSE conversation 事件使用，值为智能体 publicId；REST 列表为 null 不序列化 */
    @JsonProperty("agent_id")
    private String agentId;
}
