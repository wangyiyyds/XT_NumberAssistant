package com.xianting.manage.controller;

import com.xianting.manage.common.Result;
import com.xianting.manage.dto.PlanDepartmentsRequest;
import com.xianting.manage.dto.PlanDto;
import com.xianting.manage.dto.PlanSaveRequest;
import com.xianting.manage.service.PlanService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 套餐管理：套餐 CRUD + 套餐开放哪些部门(左侧导航)的关系配置 */
@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public Result<List<PlanDto>> list() {
        return Result.ok(planService.list());
    }

    @GetMapping("/{publicId}")
    public Result<PlanDto> detail(@PathVariable String publicId) {
        return Result.ok(planService.detail(publicId));
    }

    @PostMapping
    public Result<PlanDto> create(@RequestBody PlanSaveRequest request) {
        return Result.ok(planService.create(request));
    }

    @PutMapping("/{publicId}")
    public Result<PlanDto> update(@PathVariable String publicId, @RequestBody PlanSaveRequest request) {
        return Result.ok(planService.update(publicId, request));
    }

    /** 全量替换该套餐开放的部门列表 */
    @PutMapping("/{publicId}/departments")
    public Result<PlanDto> setDepartments(@PathVariable String publicId,
                                          @RequestBody PlanDepartmentsRequest request) {
        return Result.ok(planService.setDepartments(publicId, request.getDepartmentIds()));
    }

    @DeleteMapping("/{publicId}")
    public Result<Void> delete(@PathVariable String publicId) {
        planService.delete(publicId);
        return Result.ok(null);
    }
}
