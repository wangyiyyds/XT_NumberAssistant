package com.xianting.manage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 智能体新建/编辑请求体 */
@Data
public class AgentSaveRequest {

    /** 所属部门 publicId，必填；存在性由 Service 校验（不存在返回 404） */
    @NotBlank(message = "请选择所属部门")
    @Size(max = 64, message = "部门标识长度不能超过64")
    private String departmentId;

    /** 对应 t_agent.name VARCHAR(128) NOT NULL */
    @NotBlank(message = "智能体名称必填")
    @Size(max = 128, message = "智能体名称长度不能超过128")
    private String name;

    /** 对应 t_agent.description VARCHAR(1024) */
    @Size(max = 1024, message = "智能体描述长度不能超过1024")
    private String description;

    /** 对应 t_agent.avatar VARCHAR(512) */
    @Size(max = 512, message = "头像地址长度不能超过512")
    private String avatar;

    /** 对应 t_agent.category VARCHAR(64) */
    @Size(max = 64, message = "分类长度不能超过64")
    private String category;

    /** 是否预置（客户端按 isPreset=1 拉取智能体，管理端创建默认 true） */
    private Boolean isPreset;

    /** 开场白列表，序列化为 JSON 存入 t_agent.conversation_starters VARCHAR(2048) */
    private List<String> conversationStarters;

    /** 对应 t_agent.instructions TEXT，不设上限 */
    private String instructions;

    /** 仅创建时使用：初始提示词正文，自动落为 v1 ACTIVE；留空用默认模板 */
    private String promptContent;
}
