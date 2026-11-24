package com.xianting.manage.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.xianting.manage.entity.PlanDepartment;

import java.util.Collection;
import java.util.List;

/** 套餐-部门关系数据仓储接口，继承 MyBatis-Plus IRepository */
public interface PlanDepartmentRepository extends IRepository<PlanDepartment> {

    List<PlanDepartment> listByPlanId(Long planId);

    /** 批量取多个套餐的关系行，列表页组装用 */
    List<PlanDepartment> listByPlanIds(Collection<Long> planIds);

    /** 删除套餐时清理其全部关系行 */
    void deleteByPlanId(Long planId);

    /** 删除部门时清理其全部关系行 */
    void deleteByDepartmentId(Long departmentId);
}
