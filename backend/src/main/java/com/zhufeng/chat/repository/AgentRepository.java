package com.zhufeng.chat.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.zhufeng.chat.entity.Agent;

import java.util.List;

/** 智能体数据仓储接口，继承 MyBatis-Plus IRepository */
public interface AgentRepository extends IRepository<Agent> {

    /** 全部智能体，按 id 升序 */
    List<Agent> listAll();

    /**
     * 条件列表：按部门过滤 + 是否仅预置，按 id 升序。
     *
     * @param departmentId 部门 id，null 表示不过滤
     * @param presetOnly   true 表示仅查预置智能体
     */
    List<Agent> listByDepartment(Long departmentId, boolean presetOnly);

    Agent findByPublicId(String publicId);

}
