package com.xianting.manage.dto;

import lombok.Data;

import java.util.List;

/** 智能体新建/编辑请求体 */
@Data
public class AgentSaveRequest {
    /** 所属部门 publicId，必填 */
    private String departmentId;
    private String name;
    private String description;
    private String avatar;
    private String category;
    /** 是否预置（客户端按 isPreset=1 拉取智能体，管理端创建默认 true） */
    private Boolean isPreset;
    private List<String> conversationStarters;
    private String instructions;
    /** 仅创建时使用：初始提示词正文，自动落为 v1 ACTIVE；留空用默认模板 */
    private String promptContent;
}
