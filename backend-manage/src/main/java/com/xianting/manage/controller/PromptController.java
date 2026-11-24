package com.xianting.manage.controller;

import com.xianting.manage.common.AdminContext;
import com.xianting.manage.common.Result;
import com.xianting.manage.dto.PromptDetailDto;
import com.xianting.manage.dto.PromptDto;
import com.xianting.manage.dto.PromptSaveRequest;
import com.xianting.manage.service.AdminAuthService;
import com.xianting.manage.service.PromptService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 提示词版本管理：版本列表 / 详情 / 新建草稿 / 编辑草稿 / 发布 / 回滚。
 * 发布后客户端 AiServiceImpl 读到的 ACTIVE content 立即切换，客户端零改动。
 */
@RestController
@RequestMapping("/api")
public class PromptController {

    private final PromptService promptService;
    private final AdminAuthService adminAuthService;

    public PromptController(PromptService promptService, AdminAuthService adminAuthService) {
        this.promptService = promptService;
        this.adminAuthService = adminAuthService;
    }

    /** 某智能体的全部版本（version 降序，不含正文） */
    @GetMapping("/agents/{agentPublicId}/prompts")
    public Result<List<PromptDto>> listByAgent(@PathVariable String agentPublicId) {
        return Result.ok(promptService.listByAgent(agentPublicId));
    }

    /** 基于当前 ACTIVE 版本新建草稿（version 自动 +1） */
    @PostMapping("/agents/{agentPublicId}/prompts")
    public Result<PromptDetailDto> createDraft(@PathVariable String agentPublicId,
                                               @RequestBody PromptSaveRequest request) {
        String admin = adminAuthService.usernameOf(AdminContext.require());
        return Result.ok(promptService.createDraft(agentPublicId, request, admin));
    }

    @GetMapping("/prompts/{publicId}")
    public Result<PromptDetailDto> detail(@PathVariable String publicId) {
        return Result.ok(promptService.detail(publicId));
    }

    /** 编辑草稿（仅 DRAFT 可改） */
    @PutMapping("/prompts/{publicId}")
    public Result<PromptDetailDto> updateDraft(@PathVariable String publicId,
                                               @RequestBody PromptSaveRequest request) {
        String admin = adminAuthService.usernameOf(AdminContext.require());
        return Result.ok(promptService.updateDraft(publicId, request, admin));
    }

    /** 发布：旧 ACTIVE 自动归档，本版本置 ACTIVE */
    @PostMapping("/prompts/{publicId}/publish")
    public Result<PromptDetailDto> publish(@PathVariable String publicId) {
        String admin = adminAuthService.usernameOf(AdminContext.require());
        return Result.ok(promptService.publish(publicId, admin));
    }

    /** 回滚：以该历史版本内容生成新的 ACTIVE 版本 */
    @PostMapping("/prompts/{publicId}/rollback")
    public Result<PromptDetailDto> rollback(@PathVariable String publicId) {
        String admin = adminAuthService.usernameOf(AdminContext.require());
        return Result.ok(promptService.rollback(publicId, admin));
    }
}
