package com.xianting.manage.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 提示词版本详情 DTO = 列表字段 + 正文与 JSON 配置（原样透传，本期不提供编辑） */
@Data
@EqualsAndHashCode(callSuper = true)
public class PromptDetailDto extends PromptDto {
    /** 交给大模型的正文 */
    private String content;
    /** JSON array string，新版本创建时从 parent 复制 */
    private String variables;
    /** JSON string，新版本创建时从 parent 复制 */
    private String modelConfig;
}
