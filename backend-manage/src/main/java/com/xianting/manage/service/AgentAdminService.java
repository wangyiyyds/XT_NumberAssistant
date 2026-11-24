package com.xianting.manage.service;

import com.xianting.manage.common.PageResult;
import com.xianting.manage.dto.AgentAdminDto;
import com.xianting.manage.dto.AgentDetailDto;
import com.xianting.manage.dto.AgentSaveRequest;

public interface AgentAdminService {

    /**
     * 分页查智能体（含部门名与当前生效提示词版本号）。
     *
     * @param departmentPublicId 按部门过滤，可空
     * @param keyword            name 模糊关键字，可空
     */
    PageResult<AgentAdminDto> page(long page, long size, String departmentPublicId, String keyword);

    AgentDetailDto detail(String publicId);

    /** 创建智能体：事务内自动创建 v1 ACTIVE 提示词（部门->智能体->提示词->自动更新库） */
    AgentDetailDto create(AgentSaveRequest request, String adminUsername);

    AgentDetailDto update(String publicId, AgentSaveRequest request);

    /** 删除智能体：其全部提示词版本由 fk_prompt_agent ON DELETE CASCADE 级联清理 */
    void delete(String publicId);
}
