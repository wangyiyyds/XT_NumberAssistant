package com.zhufeng.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.zhufeng.chat.entity.Balance;
import com.zhufeng.chat.mapper.BalanceMapper;
import com.zhufeng.chat.repository.BalanceRepository;
import org.springframework.stereotype.Repository;

@Repository
public class BalanceRepositoryImpl extends CrudRepository<BalanceMapper, Balance> implements BalanceRepository {

    @Override
    public Balance findByUserId(Long userId) {
        QueryWrapper<Balance> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        return getBaseMapper().selectOne(qw);
    }

}
