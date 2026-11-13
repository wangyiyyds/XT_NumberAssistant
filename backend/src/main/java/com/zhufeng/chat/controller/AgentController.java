package com.zhufeng.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhufeng.chat.common.ApiException;
import com.zhufeng.chat.entity.Agent;
import com.zhufeng.chat.entity.Department;
import com.zhufeng.chat.mapper.AgentMapper;
import com.zhufeng.chat.mapper.DepartmentMapper;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class AgentController {

    private final AgentMapper agentMapper;
    private final DepartmentMapper departmentMapper;

    public AgentController(AgentMapper agentMapper, DepartmentMapper departmentMapper) {
        this.agentMapper = agentMapper;
        this.departmentMapper = departmentMapper;
    }

    @GetMapping("/api/departments")
    public List<Map<String, Object>> departments() {
        QueryWrapper<Department> qw = new QueryWrapper<>();
        qw.orderByAsc("sort_order");
        List<Map<String, Object>> list = new ArrayList<>();
        for (Department d : departmentMapper.selectList(qw)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("_id", d.getPublicId());
            m.put("name", d.getName());
            m.put("enabled", d.getEnabled());
            m.put("brandOnly", d.getBrandOnly());
            m.put("sortOrder", d.getSortOrder());
            m.put("extraTools", List.of());
            m.put("__v", 0);
            list.add(m);
        }
        return list;
    }

    /** OpenAI-style paginated list, like the observed target API. */
    @GetMapping("/api/agents")
    public Map<String, Object> agents(@RequestParam(required = false) String department,
                                      @RequestParam(required = false) String isPreset,
                                      @RequestParam(required = false) String requiredPermission) {
        QueryWrapper<Agent> qw = new QueryWrapper<>();
        if (department != null && !department.isBlank()) {
            Long deptId = resolveDepartmentId(department);
            qw.eq("department_id", deptId);
        }
        if ("1".equals(isPreset) || "true".equalsIgnoreCase(isPreset)) {
            qw.eq("is_preset", true);
        }
        qw.orderByAsc("id");
        List<Agent> agents = agentMapper.selectList(qw);
        Map<Long, String> deptNames = departmentNames();
        List<Map<String, Object>> data = agents.stream()
                .map(a -> toVo(a, deptNames.get(a.getDepartmentId())))
                .collect(Collectors.toList());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("object", "list");
        m.put("data", data);
        m.put("first_id", data.isEmpty() ? null : agents.get(0).getPublicId());
        m.put("last_id", data.isEmpty() ? null : agents.get(agents.size() - 1).getPublicId());
        m.put("has_more", false);
        m.put("after", null);
        return m;
    }

    @GetMapping("/api/agents/my-access")
    public Map<String, Object> myAccess() {
        QueryWrapper<Agent> qw = new QueryWrapper<>();
        qw.orderByAsc("id");
        List<String> ids = agentMapper.selectList(qw).stream().map(Agent::getPublicId).collect(Collectors.toList());
        return Map.of("data", ids);
    }

    @GetMapping("/api/agents/chat/active")
    public List<Map<String, Object>> chatActive() {
        return List.of();
    }

    @GetMapping("/api/agents/{publicId}")
    public Map<String, Object> agentDetail(@PathVariable String publicId) {
        Agent agent = findByPublicId(publicId);
        return toVo(agent, departmentNames().get(agent.getDepartmentId()));
    }

    public Agent findByPublicId(String publicId) {
        QueryWrapper<Agent> qw = new QueryWrapper<>();
        qw.eq("public_id", publicId);
        Agent agent = agentMapper.selectOne(qw);
        if (agent == null) throw new ApiException(404, "agent not found: " + publicId);
        return agent;
    }

    public static Map<String, Object> toVo(Agent a, String departmentName) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_id", a.getPublicId());
        m.put("agent_id", a.getPublicId());
        m.put("name", a.getName());
        m.put("description", a.getDescription());
        m.put("avatar", a.getAvatar());
        m.put("category", a.getCategory());
        m.put("department", departmentName);
        m.put("isPreset", a.getIsPreset());
        m.put("conversation_starters", parseStarters(a.getConversationStarters()));
        m.put("instructions", a.getInstructions());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }

    static List<String> parseStarters(String json) {
        if (json == null || json.isBlank()) return List.of();
        String s = json.trim();
        if (!s.startsWith("[")) return List.of();
        List<String> out = new ArrayList<>();
        for (String part : s.substring(1, s.length() - 1).split("\",\"")) {
            String v = part.replace("\"", "").trim();
            if (!v.isEmpty()) out.add(v);
        }
        return out;
    }

    private Long resolveDepartmentId(String department) {
        QueryWrapper<Department> qw = new QueryWrapper<>();
        qw.eq("name", department).or().eq("public_id", department);
        Department d = departmentMapper.selectOne(qw);
        if (d == null) throw new ApiException(404, "department not found: " + department);
        return d.getId();
    }

    private Map<Long, String> departmentNames() {
        Map<Long, String> m = new HashMap<>();
        for (Department d : departmentMapper.selectList(null)) {
            m.put(d.getId(), d.getName());
        }
        return m;
    }
}
