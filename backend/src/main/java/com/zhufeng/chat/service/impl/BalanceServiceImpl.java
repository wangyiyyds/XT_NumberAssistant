package com.zhufeng.chat.service.impl;

import com.zhufeng.chat.common.ApiException;
import com.zhufeng.chat.converter.BalanceConverter;
import com.zhufeng.chat.dto.BalanceDto;
import com.zhufeng.chat.entity.Balance;
import com.zhufeng.chat.entity.User;
import com.zhufeng.chat.repository.BalanceRepository;
import com.zhufeng.chat.service.BalanceService;
import com.zhufeng.chat.service.UserService;
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
    public void deduct(Long userId, long cost) {
        Balance balance = balanceRepository.findByUserId(userId);
        if (balance == null) return;
        long credits = balance.getTokenCredits() == null ? 0 : balance.getTokenCredits();
        balance.setTokenCredits(Math.max(0, credits - cost));
        balanceRepository.updateById(balance);
    }

    @Override
    public void deductOrThrow(Long userId, long cost) {
        Balance balance = balanceRepository.findByUserId(userId);
        long credits = balance == null || balance.getTokenCredits() == null ? 0 : balance.getTokenCredits();
        if (credits < cost) {
            throw new ApiException(402, "积分不足，刷新需要" + cost + "积分");
        }
        balance.setTokenCredits(credits - cost);
        balanceRepository.updateById(balance);
    }
}
