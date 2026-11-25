package com.xianting.manage.controller;

import com.xianting.manage.common.AdminContext;
import com.xianting.manage.common.Result;
import com.xianting.manage.dto.AdminDto;
import com.xianting.manage.dto.LoginRequest;
import com.xianting.manage.service.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(adminAuthService.login(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/me")
    public Result<AdminDto> me() {
        return Result.ok(adminAuthService.me(AdminContext.require()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok(null);
    }
}
