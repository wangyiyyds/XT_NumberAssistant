package com.zhufeng.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhufeng.chat.common.*;
import com.zhufeng.chat.entity.Balance;
import com.zhufeng.chat.entity.User;
import com.zhufeng.chat.mapper.BalanceMapper;
import com.zhufeng.chat.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserMapper userMapper;
    private final BalanceMapper balanceMapper;

    @Value("${app.jwt.secret}")
    private String secret;
    @Value("${app.jwt.expire-hours}")
    private long expireHours;
    @Value("${app.balance.start}")
    private long startBalance;

    public AuthController(UserMapper userMapper, BalanceMapper balanceMapper) {
        this.userMapper = userMapper;
        this.balanceMapper = balanceMapper;
    }

    public static Map<String, Object> toVo(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("_id", u.getPublicId());
        m.put("brand", u.getBrand());
        m.put("email", u.getEmail());
        m.put("name", u.getName());
        m.put("avatar", u.getAvatar());
        m.put("emailVerified", u.getEmailVerified());
        m.put("disabled", u.getDisabled());
        m.put("contractReadAt", u.getContractReadAt());
        m.put("createdAt", u.getCreatedAt());
        return m;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        if (email == null || password == null) {
            throw new ApiException(400, "email and password are required");
        }
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("email", email.trim().toLowerCase());
        User user = userMapper.selectOne(qw);
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            throw new ApiException(401, "邮箱或密码错误");
        }
        if (Boolean.TRUE.equals(user.getDisabled())) {
            throw new ApiException(403, "账号已禁用");
        }
        String token = TokenUtil.issue(user.getId(), secret, expireHours);
        return Map.of("token", token, "user", toVo(user));
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String name = body.getOrDefault("name", "新用户");
        if (email == null || password == null || password.length() < 6) {
            throw new ApiException(400, "邮箱必填，密码至少6位");
        }
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("email", email.trim().toLowerCase());
        if (userMapper.selectCount(qw) > 0) {
            throw new ApiException(409, "该邮箱已注册");
        }
        User user = new User();
        user.setPublicId(UUID.randomUUID().toString());
        user.setBrand(body.getOrDefault("brand", "zhufeng"));
        user.setEmail(email.trim().toLowerCase());
        user.setPassword(PasswordUtil.hash(password));
        user.setName(name);
        user.setEmailVerified(false);
        user.setDisabled(false);
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);

        Balance balance = new Balance();
        balance.setUserId(user.getId());
        balance.setTokenCredits(startBalance);
        balance.setAutoRefillEnabled(true);
        balance.setExpireAt(LocalDateTime.now().plusYears(1));
        balanceMapper.insert(balance);

        String token = TokenUtil.issue(user.getId(), secret, expireHours);
        return Map.of("token", token, "user", toVo(user));
    }

    @PostMapping("/refresh")
    public Map<String, Object> refresh() {
        Long userId = UserContext.require();
        String token = TokenUtil.issue(userId, secret, expireHours);
        return Map.of("token", token);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        return Map.of("success", true);
    }
}
