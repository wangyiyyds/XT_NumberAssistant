package com.xianting.chat.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.xianting.chat.entity.Prompt;

/** 提示词版本数据仓储接口，继承 MyBatis-Plus IRepository */
public interface PromptRepository extends IRepository<Prompt> {

    /**
     * 取指定智能体当前生效（status='ACTIVE'）的提示词版本。
     * 表上唯一索引 uk_agent_active 保证每个智能体至多一条 ACTIVE，故可安全 selectOne；
     * 该智能体没有 ACTIVE 版本时返回 null。
     *
     * @param agentId 智能体主键 id
     */
    Prompt findActiveByAgentId(Long agentId);
}
