package com.xianting.manage.dto;

import lombok.Data;

/** 提示词草稿新建/编辑请求体（仅 DRAFT 可编辑） */
@Data
public class PromptSaveRequest {
    private String name;
    private String description;
    private String versionTag;
    /** 正文，必填 */
    private String content;
}
