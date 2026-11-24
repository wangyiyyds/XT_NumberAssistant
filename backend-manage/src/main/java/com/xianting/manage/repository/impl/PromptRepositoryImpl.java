package com.xianting.manage.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.xianting.manage.entity.Prompt;
import com.xianting.manage.mapper.PromptMapper;
import com.xianting.manage.repository.PromptRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class PromptRepositoryImpl extends CrudRepository<PromptMapper, Prompt> implements PromptRepository {

    @Override
    public Prompt findByPublicId(String publicId) {
        //  WHERE public_id = {publicId}
        QueryWrapper<Prompt> qw = new QueryWrapper<>();
        qw.eq("public_id", publicId);
        return getBaseMapper().selectOne(qw);
    }

    @Override
    public List<Prompt> listByAgentIdOrderByVersionDesc(Long agentId) {
        //  WHERE agent_id = {agentId} ORDER BY version DESC
        QueryWrapper<Prompt> qw = new QueryWrapper<>();
        qw.eq("agent_id", agentId)
          .orderByDesc("version");
        return getBaseMapper().selectList(qw);
    }

    @Override
    public Prompt findActiveByAgentId(Long agentId) {
        // 只按 agent_id + status 查：uk_agent_active 已保证一个智能体至多一条 ACTIVE
        //  WHERE agent_id = {agentId} AND status = 'ACTIVE'
        QueryWrapper<Prompt> qw = new QueryWrapper<>();
        qw.eq("agent_id", agentId)
          .eq("status", "ACTIVE");
        return getBaseMapper().selectOne(qw);
    }

    @Override
    public List<Prompt> listActiveByAgentIds(Collection<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) return List.of();
        //  WHERE agent_id IN (...) AND status = 'ACTIVE'
        QueryWrapper<Prompt> qw = new QueryWrapper<>();
        qw.in("agent_id", agentIds)
          .eq("status", "ACTIVE");
        return getBaseMapper().selectList(qw);
    }
}
