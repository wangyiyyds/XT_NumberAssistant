package com.xianting.manage.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.xianting.manage.entity.Prompt;

import java.util.Collection;
import java.util.List;

/** 提示词版本数据仓储接口，继承 MyBatis-Plus IRepository */
public interface PromptRepository extends IRepository<Prompt> {

    Prompt findByPublicId(String publicId);

    /** 某智能体的全部版本，version 降序（版本列表页） */
    List<Prompt> listByAgentIdOrderByVersionDesc(Long agentId);

    /**
     * 取指定智能体当前生效（status='ACTIVE'）的提示词版本。
     * 表上唯一索引 uk_agent_active 保证每个智能体至多一条 ACTIVE，故可安全 selectOne；
     * 该智能体没有 ACTIVE 版本时返回 null。
     */
    Prompt findActiveByAgentId(Long agentId);

    /** 批量取多个智能体的 ACTIVE 版本，智能体列表页展示"当前生效版本"用 */
    List<Prompt> listActiveByAgentIds(Collection<Long> agentIds);
}
