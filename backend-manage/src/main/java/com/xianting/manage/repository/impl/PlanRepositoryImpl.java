package com.xianting.manage.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.xianting.manage.entity.Plan;
import com.xianting.manage.mapper.PlanMapper;
import com.xianting.manage.repository.PlanRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PlanRepositoryImpl extends CrudRepository<PlanMapper, Plan> implements PlanRepository {

    @Override
    public List<Plan> listAllOrderByLevel() {
        //  ORDER BY level ASC
        QueryWrapper<Plan> qw = new QueryWrapper<>();
        qw.orderByAsc("level");
        return getBaseMapper().selectList(qw);
    }

    @Override
    public Plan findByPublicId(String publicId) {
        //  WHERE public_id = {publicId}
        QueryWrapper<Plan> qw = new QueryWrapper<>();
        qw.eq("public_id", publicId);
        return getBaseMapper().selectOne(qw);
    }

    @Override
    public long countByLevel(Integer level) {
        //  WHERE level = {level}
        QueryWrapper<Plan> qw = new QueryWrapper<>();
        qw.eq("level", level);
        return getBaseMapper().selectCount(qw);
    }
}
