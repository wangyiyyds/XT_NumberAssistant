package com.xianting.manage.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.xianting.manage.entity.Balance;

import java.util.Collection;
import java.util.List;

/** 积分数据仓储接口（管理端只读），继承 MyBatis-Plus IRepository */
public interface BalanceRepository extends IRepository<Balance> {

    /** 批量取一页用户的积分行，用户列表组装用 */
    List<Balance> listByUserIds(Collection<Long> userIds);
}
