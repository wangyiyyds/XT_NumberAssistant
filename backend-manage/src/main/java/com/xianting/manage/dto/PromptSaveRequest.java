package com.xianting.manage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 提示词草稿新建/编辑请求体（仅 DRAFT 可编辑） */
@Data
public class PromptSaveRequest {

    /** 对应 t_prompt.name VARCHAR(128) */
    @Size(max = 128, message = "版本名称长度不能超过128")
    private String name;

    /** 对应 t_prompt.description VARCHAR(1024) */
    @Size(max = 1024, message = "版本说明长度不能超过1024")
    private String description;

    /** 对应 t_prompt.version_tag VARCHAR(32) */
    @Size(max = 32, message = "版本标签长度不能超过32")
    private String versionTag;

    /** 正文，必填；列为 MEDIUMTEXT，不设长度上限 */
    @NotBlank(message = "提示词正文必填")
    private String content;
}
