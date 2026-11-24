package com.xianting.manage.service;

import com.xianting.manage.dto.PlanDto;
import com.xianting.manage.dto.PlanSaveRequest;

import java.util.List;

public interface PlanService {

    /** 全部套餐（等级升序），含各套餐开放的部门列表 */
    List<PlanDto> list();

    PlanDto detail(String publicId);

    PlanDto create(PlanSaveRequest request);

    PlanDto update(String publicId, PlanSaveRequest request);

    /** 全量替换套餐开放的部门（departmentPublicIds 为部门 publicId 列表） */
    PlanDto setDepartments(String publicId, List<String> departmentPublicIds);

    /** 删除套餐：仍有用户在用时拒绝；事务内同步清理套餐-部门关系 */
    void delete(String publicId);
}
