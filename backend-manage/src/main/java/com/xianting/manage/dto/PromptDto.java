package com.xianting.manage.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提示词版本列表行 DTO：不含 content（mediumtext 不进列表），
 * 正文用 PromptDetailDto 单查。
 */
@Data
public class PromptDto {
    /** 对应实体 publicId */
    private String id;
    /** 归属智能体 publicId（跨表数据，由转换器额外入参提供） */
    private String agentId;
    private Integer version;
    private String versionTag;
    private String name;
    private String description;
    private String promptType;
    /** DRAFT / ACTIVE / ARCHIVED */
    private String status;
    /** 血缘：基于哪个版本号改出来的，首版为 null（由 Service 批量解析 parent 后注入） */
    private Integer parentVersion;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime archivedAt;
}
