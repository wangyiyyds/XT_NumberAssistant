package com.xianting.manage.service.impl;

import com.xianting.manage.common.ApiException;
import com.xianting.manage.converter.DepartmentConverter;
import com.xianting.manage.converter.PlanConverter;
import com.xianting.manage.dto.DepartmentDto;
import com.xianting.manage.dto.PlanDto;
import com.xianting.manage.dto.PlanSaveRequest;
import com.xianting.manage.entity.Department;
import com.xianting.manage.entity.Plan;
import com.xianting.manage.entity.PlanDepartment;
import com.xianting.manage.repository.DepartmentRepository;
import com.xianting.manage.repository.PlanDepartmentRepository;
import com.xianting.manage.repository.PlanRepository;
import com.xianting.manage.repository.UserRepository;
import com.xianting.manage.service.PlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;
    private final PlanDepartmentRepository planDepartmentRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PlanConverter planConverter;
    private final DepartmentConverter departmentConverter;

    public PlanServiceImpl(PlanRepository planRepository, PlanDepartmentRepository planDepartmentRepository,
                           DepartmentRepository departmentRepository, UserRepository userRepository,
                           PlanConverter planConverter, DepartmentConverter departmentConverter) {
        this.planRepository = planRepository;
        this.planDepartmentRepository = planDepartmentRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.planConverter = planConverter;
        this.departmentConverter = departmentConverter;
    }

    @Override
    public List<PlanDto> list() {
        List<Plan> plans = planRepository.listAllOrderByLevel();
        if (plans.isEmpty()) return List.of();
        // 批量取关系行与部门，内存按 planId 分组组装，避免 N+1
        List<Long> planIds = plans.stream().map(Plan::getId).toList();
        Map<Long, List<Long>> deptIdsByPlan = planDepartmentRepository.listByPlanIds(planIds).stream()
                .collect(Collectors.groupingBy(PlanDepartment::getPlanId,
                        Collectors.mapping(PlanDepartment::getDepartmentId, Collectors.toList())));
        Map<Long, Department> deptById = loadDepartments(deptIdsByPlan.values().stream()
                .flatMap(List::stream).collect(Collectors.toSet()));
        List<PlanDto> out = new ArrayList<>();
        for (Plan plan : plans) {
            out.add(planConverter.toDto(plan, toDepartmentDtos(deptIdsByPlan.get(plan.getId()), deptById)));
        }
        return out;
    }

    @Override
    public PlanDto detail(String publicId) {
        Plan plan = requireByPublicId(publicId);
        List<Long> deptIds = planDepartmentRepository.listByPlanId(plan.getId()).stream()
                .map(PlanDepartment::getDepartmentId).toList();
        return planConverter.toDto(plan, toDepartmentDtos(deptIds, loadDepartments(deptIds)));
    }

    @Override
    public PlanDto create(PlanSaveRequest request) {
        validate(request);
        if (planRepository.countByLevel(request.getLevel()) > 0) {
            throw new ApiException(409, "套餐等级 " + request.getLevel() + " 已存在");
        }
        Plan plan = planConverter.toEntity(request);
        plan.setPublicId(UUID.randomUUID().toString());
        planRepository.save(plan);
        return detail(plan.getPublicId());
    }

    @Override
    public PlanDto update(String publicId, PlanSaveRequest request) {
        Plan plan = requireByPublicId(publicId);
        validate(request);
        // uk_plan_level 全表唯一：等级变化时确认没有被别的套餐占用
        if (!plan.getLevel().equals(request.getLevel())) {
            Plan holder = planRepository.listAllOrderByLevel().stream()
                    .filter(p -> p.getLevel().equals(request.getLevel()))
                    .findFirst().orElse(null);
            if (holder != null && !holder.getId().equals(plan.getId())) {
                throw new ApiException(409, "套餐等级 " + request.getLevel() + " 已被「" + holder.getName() + "」占用");
            }
        }
        plan.setName(request.getName().trim());
        plan.setLevel(request.getLevel());
        plan.setDescription(request.getDescription());
        planRepository.updateById(plan);
        return detail(publicId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanDto setDepartments(String publicId, List<String> departmentPublicIds) {
        Plan plan = requireByPublicId(publicId);
        // 去重保序，逐个解析部门 publicId，不存在直接 404
        LinkedHashSet<String> distinctIds = departmentPublicIds == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(departmentPublicIds);
        List<Long> deptIds = new ArrayList<>();
        for (String deptPublicId : distinctIds) {
            Department dept = departmentRepository.findByPublicId(deptPublicId);
            if (dept == null) throw new ApiException(404, "部门不存在: " + deptPublicId);
            deptIds.add(dept.getId());
        }
        // 全量替换：先删后插，uk_plan_department 防重
        planDepartmentRepository.deleteByPlanId(plan.getId());
        for (Long deptId : deptIds) {
            PlanDepartment rel = new PlanDepartment();
            rel.setPlanId(plan.getId());
            rel.setDepartmentId(deptId);
            planDepartmentRepository.save(rel);
        }
        return detail(publicId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String publicId) {
        Plan plan = requireByPublicId(publicId);
        long users = userRepository.countByPlanId(plan.getId());
        if (users > 0) {
            throw new ApiException(409, "该套餐下仍有 " + users + " 个用户，请先调整用户套餐");
        }
        planDepartmentRepository.deleteByPlanId(plan.getId());
        planRepository.removeById(plan.getId());
    }

    private void validate(PlanSaveRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ApiException(400, "套餐名称必填");
        }
        if (request.getLevel() == null) {
            throw new ApiException(400, "套餐等级必填");
        }
    }

    private Plan requireByPublicId(String publicId) {
        Plan plan = planRepository.findByPublicId(publicId);
        if (plan == null) throw new ApiException(404, "套餐不存在");
        return plan;
    }

    private Map<Long, Department> loadDepartments(java.util.Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return Map.of();
        return departmentRepository.listByIds(deptIds).stream()
                .collect(Collectors.toMap(Department::getId, Function.identity()));
    }

    /** 关系行里的部门 id 列表 -> 按 sortOrder 升序的 DepartmentDto 列表 */
    private List<DepartmentDto> toDepartmentDtos(List<Long> deptIds, Map<Long, Department> deptById) {
        if (deptIds == null || deptIds.isEmpty()) return List.of();
        return deptIds.stream()
                .map(deptById::get)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(d -> d.getSortOrder() == null ? 0 : d.getSortOrder()))
                .map(departmentConverter::toDto)
                .toList();
    }
}
