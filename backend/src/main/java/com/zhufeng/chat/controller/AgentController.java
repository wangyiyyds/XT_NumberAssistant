package com.zhufeng.chat.controller;

import com.zhufeng.chat.dto.AgentDto;
import com.zhufeng.chat.dto.DepartmentDto;
import com.zhufeng.chat.service.AgentService;
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
    public List<DepartmentDto> departments() {
        return agentService.listDepartments();
    }

    /** OpenAI-style paginated list, like the observed target API. */
    @GetMapping("/api/agents")
    public Map<String, Object> agents(@RequestParam(required = false) String department,
                                      @RequestParam(required = false) String isPreset,
                                      @RequestParam(required = false) String requiredPermission) {
        return agentService.listAgents(department, isPreset);
    }

    @GetMapping("/api/agents/my-access")
    public Map<String, Object> myAccess() {
        return agentService.myAccess();
    }

    @GetMapping("/api/agents/chat/active")
    public List<Map<String, Object>> chatActive() {
        return List.of();
    }

    @GetMapping("/api/agents/{publicId}")
    public AgentDto agentDetail(@PathVariable String publicId) {
        return agentService.detail(publicId);
    }
}
