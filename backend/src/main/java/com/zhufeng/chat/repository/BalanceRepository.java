package com.zhufeng.chat.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.zhufeng.chat.entity.Balance;

/** 余额数据仓储接口，继承 MyBatis-Plus IRepository */
public interface BalanceRepository extends IRepository<Balance> {

    Balance findByUserId(Long userId);

}
