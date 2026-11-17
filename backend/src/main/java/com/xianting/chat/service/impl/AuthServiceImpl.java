package com.xianting.chat.service.impl;

import com.xianting.chat.common.ApiException;
import com.xianting.chat.common.PasswordUtil;
import com.xianting.chat.common.TokenUtil;
import com.xianting.chat.converter.UserConverter;
import com.xianting.chat.entity.Balance;
import com.xianting.chat.entity.User;
import com.xianting.chat.repository.BalanceRepository;
import com.xianting.chat.repository.UserRepository;
import com.xianting.chat.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final BalanceRepository balanceRepository;
    private final UserConverter userConverter;

    @Value("${app.jwt.secret}")
    private String secret;
    @Value("${app.jwt.expire-hours}")
    private long expireHours;
    @Value("${app.balance.start}")
    private long startBalance;

    public AuthServiceImpl(UserRepository userRepository, BalanceRepository balanceRepository,
                           UserConverter userConverter) {
        this.userRepository = userRepository;
        this.balanceRepository = balanceRepository;
        this.userConverter = userConverter;
    }

    @Override
    public Map<String, Object> login(String email, String password) {
        if (email == null || password == null) {
            throw new ApiException(400, "email and password are required");
        }
        User user = userRepository.findByEmail(email.trim().toLowerCase());
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            throw new ApiException(401, "邮箱或密码错误");
        }
        if (Boolean.TRUE.equals(user.getDisabled())) {
            throw new ApiException(403, "账号已禁用");
        }
        return tokenWithUser(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> register(String email, String password, String name, String brand) {
        if (email == null || password == null || password.length() < 6) {
            throw new ApiException(400, "邮箱必填，密码至少6位");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(409, "该邮箱已注册");
        }
        User user = new User();
        user.setPublicId(UUID.randomUUID().toString());
        user.setBrand(brand == null ? "xianting" : brand);
        user.setEmail(normalizedEmail);
        user.setPassword(PasswordUtil.hash(password));
        user.setName(name == null ? "新用户" : name);
        user.setEmailVerified(false);
        user.setDisabled(false);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        Balance balance = new Balance();
        balance.setUserId(user.getId());
        balance.setTokenCredits(startBalance);
        balance.setAutoRefillEnabled(true);
        balance.setExpireAt(LocalDateTime.now().plusYears(1));
        balanceRepository.save(balance);

        return tokenWithUser(user);
    }

    @Override
    public Map<String, Object> refresh(Long userId) {
        return Map.of("token", TokenUtil.issue(userId, secret, expireHours));
    }

    private Map<String, Object> tokenWithUser(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", TokenUtil.issue(user.getId(), secret, expireHours));
        m.put("user", userConverter.toDto(user));
        return m;
    }
}
