package com.xianting.chat.service.impl;

import com.xianting.chat.common.ApiException;
import com.xianting.chat.converter.BalanceConverter;
import com.xianting.chat.dto.BalanceDto;
import com.xianting.chat.entity.Balance;
import com.xianting.chat.entity.User;
import com.xianting.chat.repository.BalanceRepository;
import com.xianting.chat.service.BalanceService;
import com.xianting.chat.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class BalanceServiceImpl implements BalanceService {

    private final BalanceRepository balanceRepository;
    private final UserService userService;
    private final BalanceConverter balanceConverter;

    public BalanceServiceImpl(BalanceRepository balanceRepository, UserService userService,
                              BalanceConverter balanceConverter) {
        this.balanceRepository = balanceRepository;
        this.userService = userService;
        this.balanceConverter = balanceConverter;
    }

    @Override
    public BalanceDto getBalance(Long userId) {
        User user = userService.requireUser(userId);
        Balance balance = balanceRepository.findByUserId(userId);
        return balanceConverter.toDto(balance, user.getPublicId());
    }

    @Override
    public void deductOrThrow(Long userId, long cost) {
        // 一条原子 UPDATE 完成「判断 + 扣减」，影响行数为 0 说明余额不足。
        // 不再采用旧的"先查出余额、内存里减、再写回"三步走——那种写法并发下会丢失更新。
        if (balanceRepository.deductIfEnough(userId, cost) == 0) {
            throw new ApiException(402, "积分不足，需要" + cost + "积分");
        }
    }
}
