package com.xianting.manage.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.xianting.manage.entity.Agent;

/** 智能体数据仓储接口（管理端视角），继承 MyBatis-Plus IRepository */
public interface AgentRepository extends IRepository<Agent> {

    /**
     * 分页查智能体。
     *
     * @param departmentId 部门主键过滤，可空
     * @param keyword      匹配 name 的 LIKE 关键字，可空
     */
    Page<Agent> pageByFilter(long current, long size, Long departmentId, String keyword);

    Agent findByPublicId(String publicId);

    /** 删除部门前检查其下智能体数量 */
    long countByDepartmentId(Long departmentId);
}
