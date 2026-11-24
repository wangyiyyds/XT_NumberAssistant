package com.xianting.manage.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 管理端智能体详情 DTO = 列表字段 + instructions + 当前生效提示词 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentDetailDto extends AgentAdminDto {
    /** 旧人设字段，无 ACTIVE 提示词时客户端用它兜底 */
    private String instructions;
    /** 当前生效（ACTIVE）的提示词版本，从未发布过为 null；由 Service 组装 */
    private PromptDetailDto activePrompt;
}
