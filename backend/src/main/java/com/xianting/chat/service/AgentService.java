package com.xianting.chat.service;

import com.xianting.chat.dto.AgentDto;
import com.xianting.chat.dto.DepartmentDto;
import com.xianting.chat.entity.Agent;

import java.util.List;
import java.util.Map;

/** 智能体/部门业务接口 */
public interface AgentService {

    /** 部门列表 DTO */
    List<DepartmentDto> listDepartments();

    /** OpenAI 风格分页列表 */
    Map<String, Object> listAgents(String department, String isPreset);

    /** 当前用户可见的智能体 publicId 列表，返回 {data: [...]} */
    Map<String, Object> myAccess();

    /** 智能体详情 DTO */
    AgentDto detail(String publicId);

    /** 按 publicId 查实体，不存在抛 404 */
    Agent requireByPublicId(String publicId);
}
