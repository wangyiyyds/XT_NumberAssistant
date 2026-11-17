package com.xianting.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 智能体 DTO，对外字段与原接口一致 */
@Data
public class AgentDto {
    /** 对应实体 publicId */
    @JsonProperty("_id")
    private String id;
    /** 同 publicId，兼容旧接口的 agent_id 字段 */
    @JsonProperty("agent_id")
    private String agentId;
    private String name;
    private String description;
    private String avatar;
    private String category;
    /** 部门名称（跨表数据，由转换器第二个入参提供） */
    private String department;
    private Boolean isPreset;
    /** 实体里是 JSON 字符串，这里是解析后的列表 */
    @JsonProperty("conversation_starters")
    private List<String> conversationStarters;
    private String instructions;
    private LocalDateTime createdAt;
}
