package com.xianting.chat.controller;

import com.xianting.chat.common.Result;
import com.xianting.chat.dto.AgentDto;
import com.xianting.chat.dto.DepartmentDto;
import com.xianting.chat.service.AgentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping("/api/departments")
    public Result<List<DepartmentDto>> departments() {
        return Result.ok(agentService.listDepartments());
    }

    /** OpenAI-style paginated list, like the observed target API. */
    @GetMapping("/api/agents")
    public Result<Map<String, Object>> agents(@RequestParam(required = false) String department,
                                              @RequestParam(required = false) String isPreset,
                                              @RequestParam(required = false) String requiredPermission) {
        return Result.ok(agentService.listAgents(department, isPreset));
    }

    @GetMapping("/api/agents/my-access")
    public Result<Map<String, Object>> myAccess() {
        return Result.ok(agentService.myAccess());
    }

    @GetMapping("/api/agents/chat/active")
    public Result<List<Map<String, Object>>> chatActive() {
        return Result.ok(List.of());
    }

    @GetMapping("/api/agents/{publicId}")
    public Result<AgentDto> agentDetail(@PathVariable String publicId) {
        return Result.ok(agentService.detail(publicId));
    }
}
