package com.xianting.manage.controller;

import com.xianting.manage.common.Result;
import com.xianting.manage.dto.DepartmentDto;
import com.xianting.manage.dto.DepartmentSaveRequest;
import com.xianting.manage.service.DepartmentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 部门管理：部门即客户端左侧导航的功能分组，套餐通过 t_plan_department 决定开放哪些部门 */
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public Result<List<DepartmentDto>> list() {
        return Result.ok(departmentService.list());
    }

    @PostMapping
    public Result<DepartmentDto> create(@RequestBody DepartmentSaveRequest request) {
        return Result.ok(departmentService.create(request));
    }

    @PutMapping("/{publicId}")
    public Result<DepartmentDto> update(@PathVariable String publicId, @RequestBody DepartmentSaveRequest request) {
        return Result.ok(departmentService.update(publicId, request));
    }

    @DeleteMapping("/{publicId}")
    public Result<Void> delete(@PathVariable String publicId) {
        departmentService.delete(publicId);
        return Result.ok(null);
    }
}
