package com.xianting.chat.mapper;

import com.xianting.chat.config.MyBaseMapper;
import com.xianting.chat.entity.Balance;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface BalanceMapper extends MyBaseMapper<Balance> {

    /**
     * 原子扣减：把「判断余额够不够 + 扣减」合并成一条 UPDATE，
     * 靠数据库的行锁保证并发下不会有两个请求读到同一个旧值、互相覆盖扣减结果。
     *
     * 原理：WHERE token_credits >= #{cost} 意味着"够扣才会命中这一行"；
     * 余额不足时这条 UPDATE 匹配不到任何行，什么都不会改。
     *
     * @return 影响行数：1 = 扣减成功；0 = 余额不足（或该用户没有余额记录）
     */
    @Update("UPDATE t_balance SET token_credits = token_credits - #{cost} " +
            "WHERE user_id = #{userId} AND token_credits >= #{cost}")
    int deductIfEnough(@Param("userId") Long userId, @Param("cost") long cost);
}
