package com.xianting.chat.service.impl;

import com.xianting.chat.common.ApiException;
import com.xianting.chat.converter.AgentConverter;
import com.xianting.chat.converter.DepartmentConverter;
import com.xianting.chat.dto.AgentDto;
import com.xianting.chat.dto.DepartmentDto;
import com.xianting.chat.entity.Agent;
import com.xianting.chat.entity.Department;
import com.xianting.chat.repository.AgentRepository;
import com.xianting.chat.repository.DepartmentRepository;
import com.xianting.chat.service.AgentService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {

    private final AgentRepository agentRepository;
    private final DepartmentRepository departmentRepository;
    private final AgentConverter agentConverter;
    private final DepartmentConverter departmentConverter;

    public AgentServiceImpl(AgentRepository agentRepository, DepartmentRepository departmentRepository,
                            AgentConverter agentConverter, DepartmentConverter departmentConverter) {
        this.agentRepository = agentRepository;
        this.departmentRepository = departmentRepository;
        this.agentConverter = agentConverter;
        this.departmentConverter = departmentConverter;
    }

    @Override
    public List<DepartmentDto> listDepartments() {
        return departmentRepository.listAllOrderBySortOrder().stream()
                .map(departmentConverter::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> listAgents(String department, String isPreset) {
        Long deptId = null;
        if (department != null && !department.isBlank()) {
            Department d = departmentRepository.findByNameOrPublicId(department);
            if (d == null) throw new ApiException(404, "department not found: " + department);
            deptId = d.getId();
        }
        boolean presetOnly = "1".equals(isPreset) || "true".equalsIgnoreCase(isPreset);
        List<Agent> agents = agentRepository.listByDepartment(deptId, presetOnly);
        Map<Long, String> deptNames = departmentNames();
        List<AgentDto> data = agents.stream()
                .map(a -> agentConverter.toDto(a, deptNames.get(a.getDepartmentId())))
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

    @Override
    public Map<String, Object> myAccess() {
        List<String> ids = agentRepository.listAll().stream()
                .map(Agent::getPublicId)
                .collect(Collectors.toList());
        return Map.of("data", ids);
    }

    @Override
    public AgentDto detail(String publicId) {
        Agent agent = requireByPublicId(publicId);
        return agentConverter.toDto(agent, departmentNames().get(agent.getDepartmentId()));
    }

    @Override
    public Agent requireByPublicId(String publicId) {
        Agent agent = agentRepository.findByPublicId(publicId);
        if (agent == null) throw new ApiException(404, "agent not found: " + publicId);
        return agent;
    }

    private Map<Long, String> departmentNames() {
        Map<Long, String> m = new HashMap<>();
        for (Department d : departmentRepository.listAll()) {
            m.put(d.getId(), d.getName());
        }
        return m;
    }
}
