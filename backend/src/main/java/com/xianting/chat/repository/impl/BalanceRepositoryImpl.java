package com.xianting.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.xianting.chat.entity.Balance;
import com.xianting.chat.mapper.BalanceMapper;
import com.xianting.chat.repository.BalanceRepository;
import org.springframework.stereotype.Repository;

@Repository
public class BalanceRepositoryImpl extends CrudRepository<BalanceMapper, Balance> implements BalanceRepository {

    @Override
    public Balance findByUserId(Long userId) {
        QueryWrapper<Balance> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        return getBaseMapper().selectOne(qw);
    }

    @Override
    public int deductIfEnough(Long userId, long cost) {
        return getBaseMapper().deductIfEnough(userId, cost);
    }

}
