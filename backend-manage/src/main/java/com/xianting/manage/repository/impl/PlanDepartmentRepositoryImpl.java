package com.xianting.manage.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.xianting.manage.entity.PlanDepartment;
import com.xianting.manage.mapper.PlanDepartmentMapper;
import com.xianting.manage.repository.PlanDepartmentRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class PlanDepartmentRepositoryImpl extends CrudRepository<PlanDepartmentMapper, PlanDepartment>
        implements PlanDepartmentRepository {

    @Override
    public List<PlanDepartment> listByPlanId(Long planId) {
        //  WHERE plan_id = {planId}
        QueryWrapper<PlanDepartment> qw = new QueryWrapper<>();
        qw.eq("plan_id", planId);
        return getBaseMapper().selectList(qw);
    }

    @Override
    public List<PlanDepartment> listByPlanIds(Collection<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) return List.of();
        //  WHERE plan_id IN (...)
        QueryWrapper<PlanDepartment> qw = new QueryWrapper<>();
        qw.in("plan_id", planIds);
        return getBaseMapper().selectList(qw);
    }

    @Override
    public void deleteByPlanId(Long planId) {
        //  DELETE WHERE plan_id = {planId}
        QueryWrapper<PlanDepartment> qw = new QueryWrapper<>();
        qw.eq("plan_id", planId);
        getBaseMapper().delete(qw);
    }

    @Override
    public void deleteByDepartmentId(Long departmentId) {
        //  DELETE WHERE department_id = {departmentId}
        QueryWrapper<PlanDepartment> qw = new QueryWrapper<>();
        qw.eq("department_id", departmentId);
        getBaseMapper().delete(qw);
    }
}
