package com.xianting.manage.service.impl;

import com.xianting.manage.common.ApiException;
import com.xianting.manage.converter.DepartmentConverter;
import com.xianting.manage.dto.DepartmentDto;
import com.xianting.manage.dto.DepartmentSaveRequest;
import com.xianting.manage.entity.Department;
import com.xianting.manage.repository.AgentRepository;
import com.xianting.manage.repository.DepartmentRepository;
import com.xianting.manage.repository.PlanDepartmentRepository;
import com.xianting.manage.service.DepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final AgentRepository agentRepository;
    private final PlanDepartmentRepository planDepartmentRepository;
    private final DepartmentConverter departmentConverter;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository, AgentRepository agentRepository,
                                 PlanDepartmentRepository planDepartmentRepository,
                                 DepartmentConverter departmentConverter) {
        this.departmentRepository = departmentRepository;
        this.agentRepository = agentRepository;
        this.planDepartmentRepository = planDepartmentRepository;
        this.departmentConverter = departmentConverter;
    }

    @Override
    public List<DepartmentDto> list() {
        return departmentRepository.listAllOrderBySortOrder().stream()
                .map(departmentConverter::toDto)
                .toList();
    }

    @Override
    public DepartmentDto create(DepartmentSaveRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ApiException(400, "部门名称必填");
        }
        Department dept = departmentConverter.toEntity(request);
        dept.setPublicId(UUID.randomUUID().toString());
        dept.setName(request.getName().trim());
        if (dept.getEnabled() == null) dept.setEnabled(true);
        if (dept.getBrandOnly() == null) dept.setBrandOnly(false);
        if (dept.getSortOrder() == null) dept.setSortOrder(0);
        departmentRepository.save(dept);
        return departmentConverter.toDto(dept);
    }

    @Override
    public DepartmentDto update(String publicId, DepartmentSaveRequest request) {
        Department dept = requireByPublicId(publicId);
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ApiException(400, "部门名称必填");
        }
        dept.setName(request.getName().trim());
        // 可选项为 null 时保持原值（启停/排序由同一接口承载）
        if (request.getEnabled() != null) dept.setEnabled(request.getEnabled());
        if (request.getBrandOnly() != null) dept.setBrandOnly(request.getBrandOnly());
        if (request.getSortOrder() != null) dept.setSortOrder(request.getSortOrder());
        departmentRepository.updateById(dept);
        return departmentConverter.toDto(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String publicId) {
        Department dept = requireByPublicId(publicId);
        long agents = agentRepository.countByDepartmentId(dept.getId());
        if (agents > 0) {
            throw new ApiException(409, "该部门下仍有 " + agents + " 个智能体，无法删除");
        }
        planDepartmentRepository.deleteByDepartmentId(dept.getId());
        departmentRepository.removeById(dept.getId());
    }

    private Department requireByPublicId(String publicId) {
        Department dept = departmentRepository.findByPublicId(publicId);
        if (dept == null) throw new ApiException(404, "部门不存在");
        return dept;
    }
}
