package com.xianting.manage.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.xianting.manage.entity.Balance;
import com.xianting.manage.mapper.BalanceMapper;
import com.xianting.manage.repository.BalanceRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class BalanceRepositoryImpl extends CrudRepository<BalanceMapper, Balance> implements BalanceRepository {

    @Override
    public List<Balance> listByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        //  WHERE user_id IN (...)
        QueryWrapper<Balance> qw = new QueryWrapper<>();
        qw.in("user_id", userIds);
        return getBaseMapper().selectList(qw);
    }
}
