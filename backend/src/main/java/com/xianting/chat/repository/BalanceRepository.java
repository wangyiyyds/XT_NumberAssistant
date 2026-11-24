package com.xianting.chat.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.xianting.chat.entity.Balance;

/** 余额数据仓储接口，继承 MyBatis-Plus IRepository */
public interface BalanceRepository extends IRepository<Balance> {

    Balance findByUserId(Long userId);

    /** 原子扣减：@return 1=扣减成功，0=余额不足；原理见 {@link com.xianting.chat.mapper.BalanceMapper#deductIfEnough} */
    int deductIfEnough(Long userId, long cost);

}
