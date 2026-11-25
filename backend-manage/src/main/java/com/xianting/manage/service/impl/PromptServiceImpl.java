package com.xianting.manage.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xianting.manage.common.ApiException;
import com.xianting.manage.converter.PromptConverter;
import com.xianting.manage.dto.PromptDetailDto;
import com.xianting.manage.dto.PromptDto;
import com.xianting.manage.dto.PromptSaveRequest;
import com.xianting.manage.entity.Agent;
import com.xianting.manage.entity.Prompt;
import com.xianting.manage.mapper.PromptMapper;
import com.xianting.manage.repository.AgentRepository;
import com.xianting.manage.repository.PromptRepository;
import com.xianting.manage.service.PromptService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 提示词版本管理：状态机 DRAFT -> ACTIVE -> ARCHIVED。
 *
 * 版本迭代规则（对应需求「提示词->自动更新库，包括库中的版本自动更新迭代」）：
 * - 新建草稿即分配 version = max + 1（uk_agent_version 防撞号）
 * - 发布：事务内先把该智能体当前 ACTIVE 归档，再把草稿置 ACTIVE；
 *   两事务并发发布时由虚拟列唯一索引 uk_agent_active 兜底，后提交者 409
 * - 回滚：历史版本不可变，以目标 ARCHIVED 版本内容生成新的 ACTIVE 版本
 * - 客户端 AiServiceImpl 只读 status='ACTIVE' 的 content，发布即生效，客户端零改动
 */
@Service
public class PromptServiceImpl implements PromptService {

    private final PromptRepository promptRepository;
    private final PromptMapper promptMapper;
    private final AgentRepository agentRepository;
    private final PromptConverter promptConverter;

    public PromptServiceImpl(PromptRepository promptRepository, PromptMapper promptMapper,
                             AgentRepository agentRepository, PromptConverter promptConverter) {
        this.promptRepository = promptRepository;
        this.promptMapper = promptMapper;
        this.agentRepository = agentRepository;
        this.promptConverter = promptConverter;
    }

    @Override
    public List<PromptDto> listByAgent(String agentPublicId) {
        Agent agent = requireAgent(agentPublicId);
        List<Prompt> prompts = promptRepository.listByAgentIdOrderByVersionDesc(agent.getId());
        if (prompts.isEmpty()) return List.of();
        // 批量解析血缘版本号，避免 N+1
        Set<Long> parentIds = new HashSet<>();
        for (Prompt p : prompts) {
            if (p.getParentPromptId() != null) parentIds.add(p.getParentPromptId());
        }
        Map<Long, Integer> versionById = parentIds.isEmpty() ? Map.of()
                : promptRepository.listByIds(parentIds).stream()
                        .collect(Collectors.toMap(Prompt::getId, Prompt::getVersion, (a, b) -> a));
        return prompts.stream()
                .map(p -> promptConverter.toDto(p, agent.getPublicId(),
                        p.getParentPromptId() == null ? null : versionById.get(p.getParentPromptId())))
                .toList();
    }

    @Override
    public PromptDetailDto detail(String publicId) {
        Prompt prompt = requireByPublicId(publicId);
        Agent agent = agentRepository.getById(prompt.getAgentId());
        Integer parentVersion = null;
        if (prompt.getParentPromptId() != null) {
            Prompt parent = promptRepository.getById(prompt.getParentPromptId());
            parentVersion = parent == null ? null : parent.getVersion();
        }
        return promptConverter.toDetailDto(prompt, agent == null ? null : agent.getPublicId(), parentVersion);
    }

    @Override
    public PromptDetailDto createDraft(String agentPublicId, PromptSaveRequest request, String adminUsername) {
        Agent agent = requireAgent(agentPublicId);
        // 正文必填由 PromptSaveRequest 注解校验
        // 血缘默认挂当前 ACTIVE 版本；没有 ACTIVE 时挂最新版本（version 降序首条）
        Prompt parent = promptRepository.findActiveByAgentId(agent.getId());
        if (parent == null) {
            List<Prompt> all = promptRepository.listByAgentIdOrderByVersionDesc(agent.getId());
            parent = all.isEmpty() ? null : all.get(0);
        }

        Prompt draft = new Prompt();
        draft.setPublicId(UUID.randomUUID().toString());
        draft.setAgentId(agent.getId());
        draft.setVersion(promptMapper.maxVersionByAgentId(agent.getId()) + 1);
        draft.setVersionTag(request.getVersionTag());
        draft.setName(request.getName());
        draft.setDescription(request.getDescription());
        draft.setPromptType("SYSTEM");
        draft.setContent(request.getContent());
        if (parent != null) {
            draft.setParentPromptId(parent.getId());
            // JSON 配置从血缘版本继承，保持迭代连续性
            draft.setVariables(parent.getVariables());
            draft.setModelConfig(parent.getModelConfig());
        }
        draft.setStatus("DRAFT");
        draft.setCreatedBy(adminUsername);
        draft.setUpdatedBy(adminUsername);
        promptRepository.save(draft);
        return detail(draft.getPublicId());
    }

    @Override
    public PromptDetailDto updateDraft(String publicId, PromptSaveRequest request, String adminUsername) {
        Prompt prompt = requireByPublicId(publicId);
        if (!"DRAFT".equals(prompt.getStatus())) {
            throw new ApiException(409, "已发布版本不可修改，请另存新版本");
        }
        prompt.setName(request.getName());
        prompt.setDescription(request.getDescription());
        prompt.setVersionTag(request.getVersionTag());
        prompt.setContent(request.getContent());
        prompt.setUpdatedBy(adminUsername);
        promptRepository.updateById(prompt);
        return detail(publicId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromptDetailDto publish(String publicId, String adminUsername) {
        Prompt prompt = requireByPublicId(publicId);
        if ("ACTIVE".equals(prompt.getStatus())) {
            throw new ApiException(400, "该版本已是当前生效版本");
        }
        if ("ARCHIVED".equals(prompt.getStatus())) {
            throw new ApiException(409, "历史归档版本不可直接发布，请使用回滚");
        }
        // 先归档当前 ACTIVE（可能不存在，影响行数 0 也合法），再激活草稿；
        // 顺序不能反，否则同事务内两条 ACTIVE 会触发 uk_agent_active
        archiveCurrentActive(prompt.getAgentId(), prompt.getId(), adminUsername);
        prompt.setStatus("ACTIVE");
        prompt.setPublishedAt(LocalDateTime.now());
        prompt.setUpdatedBy(adminUsername);
        promptRepository.updateById(prompt);
        return detail(publicId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromptDetailDto rollback(String publicId, String adminUsername) {
        Prompt target = requireByPublicId(publicId);
        if ("DRAFT".equals(target.getStatus())) {
            throw new ApiException(400, "草稿尚未发布，不能回滚，请直接发布该草稿");
        }
        if ("ACTIVE".equals(target.getStatus())) {
            throw new ApiException(400, "该版本已是当前生效版本，无需回滚");
        }
        // 历史版本不可变：以其内容为蓝本生成新的 ACTIVE 版本
        archiveCurrentActive(target.getAgentId(), null, adminUsername);
        Prompt next = new Prompt();
        next.setPublicId(UUID.randomUUID().toString());
        next.setAgentId(target.getAgentId());
        next.setVersion(promptMapper.maxVersionByAgentId(target.getAgentId()) + 1);
        next.setName(target.getName());
        next.setDescription("回滚自 v" + target.getVersion());
        next.setPromptType(target.getPromptType() == null ? "SYSTEM" : target.getPromptType());
        next.setContent(target.getContent());
        next.setVariables(target.getVariables());
        next.setModelConfig(target.getModelConfig());
        next.setStatus("ACTIVE");
        next.setParentPromptId(target.getId());
        next.setPublishedAt(LocalDateTime.now());
        next.setCreatedBy(adminUsername);
        next.setUpdatedBy(adminUsername);
        promptRepository.save(next);
        return detail(next.getPublicId());
    }

    /**
     * 条件更新归档该智能体当前的 ACTIVE 版本：
     * UPDATE t_prompt SET status='ARCHIVED', archived_at=?, updated_by=?
     * WHERE agent_id=? AND status='ACTIVE' [AND id != excludeId]
     * 并发发布时后提交者在激活步骤触发 uk_agent_active -> DuplicateKeyException -> 409
     */
    private void archiveCurrentActive(Long agentId, Long excludePromptId, String adminUsername) {
        Prompt archive = new Prompt();
        archive.setStatus("ARCHIVED");
        archive.setArchivedAt(LocalDateTime.now());
        archive.setUpdatedBy(adminUsername);
        UpdateWrapper<Prompt> uw = new UpdateWrapper<>();
        uw.eq("agent_id", agentId).eq("status", "ACTIVE");
        if (excludePromptId != null) {
            uw.ne("id", excludePromptId);
        }
        promptRepository.update(archive, uw);
    }

    private Prompt requireByPublicId(String publicId) {
        if (publicId == null || publicId.isBlank()) throw new ApiException(400, "提示词标识必填");
        Prompt prompt = promptRepository.findByPublicId(publicId);
        if (prompt == null) throw new ApiException(404, "提示词版本不存在");
        return prompt;
    }

    private Agent requireAgent(String agentPublicId) {
        if (agentPublicId == null || agentPublicId.isBlank()) throw new ApiException(400, "智能体标识必填");
        Agent agent = agentRepository.findByPublicId(agentPublicId);
        if (agent == null) throw new ApiException(404, "智能体不存在");
        return agent;
    }
}
