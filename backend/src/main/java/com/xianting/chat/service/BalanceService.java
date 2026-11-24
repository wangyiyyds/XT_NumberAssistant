package com.xianting.chat.service;

import com.xianting.chat.dto.BalanceDto;

/** 余额/积分业务接口 */
public interface BalanceService {

    /** 余额 DTO */
    BalanceDto getBalance(Long userId);

    /** 原子扣减：余额不足抛 402（按条计费的对话、付费刷新等共用） */
    void deductOrThrow(Long userId, long cost);
}
