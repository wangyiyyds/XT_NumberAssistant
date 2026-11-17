package com.xianting.chat.service;

import com.xianting.chat.dto.BalanceDto;

/** 余额/积分业务接口 */
public interface BalanceService {

    /** 余额 DTO */
    BalanceDto getBalance(Long userId);

    /** 静默扣减（余额不足时扣到 0，用于按条计费的对话） */
    void deduct(Long userId, long cost);

    /** 强制扣减（余额不足抛 402，用于付费刷新等操作） */
    void deductOrThrow(Long userId, long cost);
}
