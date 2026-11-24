package com.xianting.manage.service;

import com.xianting.manage.dto.DepartmentDto;
import com.xianting.manage.dto.DepartmentSaveRequest;

import java.util.List;

public interface DepartmentService {

    /** 全部部门，按 sortOrder 升序 */
    List<DepartmentDto> list();

    DepartmentDto create(DepartmentSaveRequest request);

    DepartmentDto update(String publicId, DepartmentSaveRequest request);

    /** 删除部门：其下仍有智能体时拒绝；事务内同步清理套餐-部门关系 */
    void delete(String publicId);
}
