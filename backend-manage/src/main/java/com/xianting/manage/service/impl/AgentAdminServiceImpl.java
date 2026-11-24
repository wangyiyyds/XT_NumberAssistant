package com.xianting.manage.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xianting.manage.common.ApiException;
import com.xianting.manage.common.PageResult;
import com.xianting.manage.converter.AgentAdminConverter;
import com.xianting.manage.converter.PromptConverter;
import com.xianting.manage.dto.AgentAdminDto;
import com.xianting.manage.dto.AgentDetailDto;
import com.xianting.manage.dto.AgentSaveRequest;
import com.xianting.manage.entity.Agent;
import com.xianting.manage.entity.Department;
import com.xianting.manage.entity.Prompt;
import com.xianting.manage.repository.AgentRepository;
import com.xianting.manage.repository.DepartmentRepository;
import com.xianting.manage.repository.PromptRepository;
import com.xianting.manage.service.AgentAdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AgentAdminServiceImpl implements AgentAdminService {

    private final AgentRepository agentRepository;
    private final DepartmentRepository departmentRepository;
    private final PromptRepository promptRepository;
    private final AgentAdminConverter agentAdminConverter;
    private final PromptConverter promptConverter;
    private final ObjectMapper objectMapper;

    public AgentAdminServiceImpl(AgentRepository agentRepository, DepartmentRepository departmentRepository,
                                 PromptRepository promptRepository, AgentAdminConverter agentAdminConverter,
                                 PromptConverter promptConverter, ObjectMapper objectMapper) {
        this.agentRepository = agentRepository;
        this.departmentRepository = departmentRepository;
        this.promptRepository = promptRepository;
        this.agentAdminConverter = agentAdminConverter;
        this.promptConverter = promptConverter;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResult<AgentAdminDto> page(long page, long size, String departmentPublicId, String keyword) {
        Long departmentId = null;
        if (departmentPublicId != null && !departmentPublicId.isBlank()) {
            departmentId = requireDepartment(departmentPublicId).getId();
        }
        Page<Agent> p = agentRepository.pageByFilter(page, size, departmentId, keyword);
        List<Agent> agents = p.getRecords();
        if (agents.isEmpty()) return PageResult.of(p, List.of());

        // 分页后批量组装部门与当前生效提示词版本，避免 N+1
        Set<Long> deptIds = new HashSet<>();
        for (Agent a : agents) {
            if (a.getDepartmentId() != null) deptIds.add(a.getDepartmentId());
        }
        Map<Long, Department> deptById = deptIds.isEmpty() ? Map.of()
                : departmentRepository.listByIds(deptIds).stream()
                        .collect(Collectors.toMap(Department::getId, Function.identity()));
        List<Long> agentIds = agents.stream().map(Agent::getId).toList();
        Map<Long, Prompt> activeByAgent = promptRepository.listActiveByAgentIds(agentIds).stream()
                .collect(Collectors.toMap(Prompt::getAgentId, Function.identity(), (a, b) -> a));

        List<AgentAdminDto> rows = new ArrayList<>();
        for (Agent a : agents) {
            Department dept = a.getDepartmentId() == null ? null : deptById.get(a.getDepartmentId());
            Prompt active = activeByAgent.get(a.getId());
            rows.add(agentAdminConverter.toDto(a,
                    dept == null ? null : dept.getPublicId(),
                    dept == null ? null : dept.getName(),
                    active == null ? null : active.getVersion()));
        }
        return PageResult.of(p, rows);
    }

    @Override
    public AgentDetailDto detail(String publicId) {
        Agent agent = requireByPublicId(publicId);
        Department dept = agent.getDepartmentId() == null ? null : departmentRepository.getById(agent.getDepartmentId());
        Prompt active = promptRepository.findActiveByAgentId(agent.getId());
        AgentDetailDto dto = agentAdminConverter.toDetailDto(agent,
                dept == null ? null : dept.getPublicId(),
                dept == null ? null : dept.getName(),
                active == null ? null : active.getVersion());
        if (active != null) {
            dto.setActivePrompt(promptConverter.toDetailDto(active, agent.getPublicId(), parentVersionOf(active)));
        }
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentDetailDto create(AgentSaveRequest request, String adminUsername) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ApiException(400, "智能体名称必填");
        }
        Department dept = requireDepartment(request.getDepartmentId());

        Agent agent = agentAdminConverter.toEntity(request);
        agent.setPublicId(UUID.randomUUID().toString());
        agent.setName(request.getName().trim());
        agent.setDepartmentId(dept.getId());
        if (agent.getIsPreset() == null) agent.setIsPreset(true);   // 客户端按 isPreset=1 拉取，默认可见
        agent.setConversationStarters(toStartersJson(request.getConversationStarters()));
        agent.setCreatedAt(LocalDateTime.now());
        agentRepository.save(agent);

        // 部门->智能体->提示词->自动更新库：创建智能体时同步落 v1 ACTIVE 提示词，
        // 客户端 AiServiceImpl 立即能按 ACTIVE 版本读取，无需再依赖 instructions 兜底
        Prompt prompt = new Prompt();
        prompt.setPublicId(UUID.randomUUID().toString());
        prompt.setAgentId(agent.getId());
        prompt.setVersion(1);
        prompt.setPromptType("SYSTEM");
        String content = request.getPromptContent();
        prompt.setContent(content == null || content.isBlank()
                ? "你是「" + agent.getName() + "」，隶属于" + dept.getName() + "。请用专业、友好的中文回答用户问题。"
                : content);
        prompt.setStatus("ACTIVE");
        prompt.setPublishedAt(LocalDateTime.now());
        prompt.setCreatedBy(adminUsername);
        prompt.setUpdatedBy(adminUsername);
        promptRepository.save(prompt);

        return detail(agent.getPublicId());
    }

    @Override
    public AgentDetailDto update(String publicId, AgentSaveRequest request) {
        Agent agent = requireByPublicId(publicId);
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ApiException(400, "智能体名称必填");
        }
        Department dept = requireDepartment(request.getDepartmentId());
        agent.setName(request.getName().trim());
        agent.setDepartmentId(dept.getId());
        agent.setDescription(request.getDescription());
        agent.setAvatar(request.getAvatar());
        agent.setCategory(request.getCategory());
        agent.setInstructions(request.getInstructions());
        if (request.getIsPreset() != null) agent.setIsPreset(request.getIsPreset());
        if (request.getConversationStarters() != null) {
            agent.setConversationStarters(toStartersJson(request.getConversationStarters()));
        }
        agentRepository.updateById(agent);
        return detail(publicId);
    }

    @Override
    public void delete(String publicId) {
        Agent agent = requireByPublicId(publicId);
        // 提示词版本由 fk_prompt_agent ON DELETE CASCADE 级联删除，血缘 FK SET NULL
        agentRepository.removeById(agent.getId());
    }

    private Agent requireByPublicId(String publicId) {
        if (publicId == null || publicId.isBlank()) throw new ApiException(400, "智能体标识必填");
        Agent agent = agentRepository.findByPublicId(publicId);
        if (agent == null) throw new ApiException(404, "智能体不存在");
        return agent;
    }

    private Department requireDepartment(String departmentPublicId) {
        if (departmentPublicId == null || departmentPublicId.isBlank()) {
            throw new ApiException(400, "请选择所属部门");
        }
        Department dept = departmentRepository.findByPublicId(departmentPublicId);
        if (dept == null) throw new ApiException(404, "部门不存在: " + departmentPublicId);
        return dept;
    }

    /** 解析血缘版本的 version 号（parent 行可能已被级联清理，查不到返回 null） */
    private Integer parentVersionOf(Prompt prompt) {
        if (prompt.getParentPromptId() == null) return null;
        Prompt parent = promptRepository.getById(prompt.getParentPromptId());
        return parent == null ? null : parent.getVersion();
    }

    /** List -> JSON 数组字符串（t_agent.conversation_starters 的存储格式） */
    private String toStartersJson(List<String> starters) {
        if (starters == null) return null;
        try {
            return objectMapper.writeValueAsString(starters);
        } catch (JsonProcessingException e) {
            throw new ApiException(400, "开场白格式不正确");
        }
    }
}
