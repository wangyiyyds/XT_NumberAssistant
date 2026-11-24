package com.xianting.manage.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.xianting.manage.entity.Agent;
import com.xianting.manage.mapper.AgentMapper;
import com.xianting.manage.repository.AgentRepository;
import org.springframework.stereotype.Repository;

@Repository
public class AgentRepositoryImpl extends CrudRepository<AgentMapper, Agent> implements AgentRepository {

    @Override
    public Page<Agent> pageByFilter(long current, long size, Long departmentId, String keyword) {
        //  WHERE department_id = ? AND name LIKE ?，按创建时间倒序
        QueryWrapper<Agent> qw = new QueryWrapper<>();
        if (departmentId != null) {
            qw.eq("department_id", departmentId);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.like("name", "%" + keyword.trim() + "%");
        }
        qw.orderByDesc("created_at");
        return getBaseMapper().selectPage(new Page<>(current, size), qw);
    }

    @Override
    public Agent findByPublicId(String publicId) {
        //  WHERE public_id = {publicId}
        QueryWrapper<Agent> qw = new QueryWrapper<>();
        qw.eq("public_id", publicId);
        return getBaseMapper().selectOne(qw);
    }

    @Override
    public long countByDepartmentId(Long departmentId) {
        //  WHERE department_id = {departmentId}
        QueryWrapper<Agent> qw = new QueryWrapper<>();
        qw.eq("department_id", departmentId);
        return getBaseMapper().selectCount(qw);
    }
}
