package com.xianting.manage.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 管理端智能体列表行 DTO */
@Data
public class AgentAdminDto {
    /** 对应实体 publicId */
    private String id;
    /** 所属部门 publicId（跨表数据，由转换器额外入参提供） */
    private String departmentId;
    /** 所属部门名称（跨表数据，由转换器额外入参提供） */
    private String departmentName;
    private String name;
    private String description;
    private String avatar;
    private String category;
    private Boolean isPreset;
    /** 实体里是 JSON 字符串，这里是解析后的列表 */
    private List<String> conversationStarters;
    /** 当前生效提示词版本号，无 ACTIVE 版本为 null */
    private Integer activePromptVersion;
    private LocalDateTime createdAt;
}
