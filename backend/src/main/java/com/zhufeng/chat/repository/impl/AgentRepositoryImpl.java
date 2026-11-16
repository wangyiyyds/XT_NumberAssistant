package com.zhufeng.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.zhufeng.chat.entity.Agent;
import com.zhufeng.chat.mapper.AgentMapper;
import com.zhufeng.chat.repository.AgentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AgentRepositoryImpl extends CrudRepository<AgentMapper, Agent> implements AgentRepository {

    @Override
    public List<Agent> listAll() {
        QueryWrapper<Agent> qw = new QueryWrapper<>();
        qw.orderByAsc("id");
        return getBaseMapper().selectList(qw);
    }

    @Override
    public List<Agent> listByDepartment(Long departmentId, boolean presetOnly) {
        QueryWrapper<Agent> qw = new QueryWrapper<>();
        if (departmentId != null) {
            qw.eq("department_id", departmentId);
        }
        if (presetOnly) {
            qw.eq("is_preset", true);
        }
        qw.orderByAsc("id");
        return getBaseMapper().selectList(qw);
    }

    @Override
    public Agent findByPublicId(String publicId) {
        QueryWrapper<Agent> qw = new QueryWrapper<>();
        qw.eq("public_id", publicId);
        return getBaseMapper().selectOne(qw);
    }

}
