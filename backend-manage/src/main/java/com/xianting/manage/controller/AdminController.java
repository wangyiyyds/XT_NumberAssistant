package com.xianting.manage.controller;

import com.xianting.manage.common.Result;
import com.xianting.manage.dto.AdminDto;
import com.xianting.manage.dto.AdminSaveRequest;
import com.xianting.manage.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 管理员管理：列表 + 新增（/api/** 已由 AuthInterceptor 保护，需登录访问） */
@RestController
@RequestMapping("/api/admins")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public Result<List<AdminDto>> list() {
        return Result.ok(adminService.list());
    }

    @PostMapping
    public Result<AdminDto> create(@RequestBody AdminSaveRequest request) {
        return Result.ok(adminService.create(request));
    }
}
