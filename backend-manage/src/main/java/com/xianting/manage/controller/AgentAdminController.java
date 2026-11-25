package com.xianting.manage.controller;

import com.xianting.manage.common.AdminContext;
import com.xianting.manage.common.PageResult;
import com.xianting.manage.common.Result;
import com.xianting.manage.dto.AgentAdminDto;
import com.xianting.manage.dto.AgentDetailDto;
import com.xianting.manage.dto.AgentSaveRequest;
import com.xianting.manage.service.AdminAuthService;
import com.xianting.manage.service.AgentAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 智能体管理：创建时自动落 v1 ACTIVE 提示词（部门->智能体->提示词->自动更新库） */
@RestController
@RequestMapping("/api/agents")
public class AgentAdminController {

    private final AgentAdminService agentAdminService;
    private final AdminAuthService adminAuthService;

    public AgentAdminController(AgentAdminService agentAdminService, AdminAuthService adminAuthService) {
        this.agentAdminService = agentAdminService;
        this.adminAuthService = adminAuthService;
    }

    @GetMapping
    public Result<PageResult<AgentAdminDto>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(agentAdminService.page(page, size, departmentId, keyword));
    }

    @GetMapping("/{publicId}")
    public Result<AgentDetailDto> detail(@PathVariable String publicId) {
        return Result.ok(agentAdminService.detail(publicId));
    }

    @PostMapping
    public Result<AgentDetailDto> create(@Valid @RequestBody AgentSaveRequest request) {
        String admin = adminAuthService.usernameOf(AdminContext.require());
        return Result.ok(agentAdminService.create(request, admin));
    }

    @PutMapping("/{publicId}")
    public Result<AgentDetailDto> update(@PathVariable String publicId,
                                         @Valid @RequestBody AgentSaveRequest request) {
        return Result.ok(agentAdminService.update(publicId, request));
    }

    @DeleteMapping("/{publicId}")
    public Result<Void> delete(@PathVariable String publicId) {
        agentAdminService.delete(publicId);
        return Result.ok(null);
    }
}
