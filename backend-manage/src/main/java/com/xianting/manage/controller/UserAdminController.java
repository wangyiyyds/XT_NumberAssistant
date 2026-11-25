package com.xianting.manage.controller;

import com.xianting.manage.common.PageResult;
import com.xianting.manage.common.Result;
import com.xianting.manage.dto.AssignPlanRequest;
import com.xianting.manage.dto.CreateUserRequest;
import com.xianting.manage.dto.SetDisabledRequest;
import com.xianting.manage.dto.UserAdminDto;
import com.xianting.manage.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用户管理：分页查询、新增、分配套餐（plan_id 即用户的功能权限依据）、禁用/启用 */
@RestController
@RequestMapping("/api/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public Result<PageResult<UserAdminDto>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String planId) {
        return Result.ok(userAdminService.page(page, size, keyword, planId));
    }

    @PostMapping
    public Result<UserAdminDto> create(@Valid @RequestBody CreateUserRequest request) {
        return Result.ok(userAdminService.create(request));
    }

    /** planId 传 null 表示清除用户套餐 */
    @PutMapping("/{publicId}/plan")
    public Result<UserAdminDto> assignPlan(@PathVariable String publicId,
                                           @Valid @RequestBody AssignPlanRequest request) {
        return Result.ok(userAdminService.assignPlan(publicId, request.getPlanId()));
    }

    @PutMapping("/{publicId}/disabled")
    public Result<UserAdminDto> setDisabled(@PathVariable String publicId,
                                            @Valid @RequestBody SetDisabledRequest request) {
        return Result.ok(userAdminService.setDisabled(publicId, request.getDisabled()));
    }
}
