package com.xianting.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.xianting.chat.entity.Prompt;
import com.xianting.chat.mapper.PromptMapper;
import com.xianting.chat.repository.PromptRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PromptRepositoryImpl extends CrudRepository<PromptMapper, Prompt> implements PromptRepository {

    @Override
    public Prompt findActiveByAgentId(Long agentId) {
        // 只按 agent_id + status 查：uk_agent_active 已保证一个智能体至多一条 ACTIVE，
        // 再按 prompt_type 过滤反而会在出现其它类型的 ACTIVE 版本时静默查不到
        //  WHERE agent_id = {agentId} AND status = 'ACTIVE'
        QueryWrapper<Prompt> qw = new QueryWrapper<>();
        qw.eq("agent_id", agentId)
          .eq("status", "ACTIVE");
        return getBaseMapper().selectOne(qw);
    }
}
