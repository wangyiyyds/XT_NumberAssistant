package com.zhufeng.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 灵感关键词 DTO，对外字段与原接口一致 */
@Data
public class InspirationKeywordDto {
    /** 关键词直接用数据库 id 对外 */
    @JsonProperty("_id")
    private Long id;
    private String keyword;
    /** 对应实体 sortOrder */
    @JsonProperty("order")
    private Integer order;
    private LocalDateTime cacheExpiresAt;
    /** 关联视频列表（跨表数据，由转换器第二个入参提供） */
    private List<InspirationVideoDto> videos;
}
