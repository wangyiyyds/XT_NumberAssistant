package com.zhufeng.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhufeng.chat.common.ApiException;
import com.zhufeng.chat.common.UserContext;
import com.zhufeng.chat.entity.Balance;
import com.zhufeng.chat.entity.User;
import com.zhufeng.chat.mapper.BalanceMapper;
import com.zhufeng.chat.mapper.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Miscellaneous bootstrap endpoints observed on the target site. */
@RestController
public class CoreController {

    private final UserMapper userMapper;
    private final BalanceMapper balanceMapper;

    public CoreController(UserMapper userMapper, BalanceMapper balanceMapper) {
        this.userMapper = userMapper;
        this.balanceMapper = balanceMapper;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/api/config")
    public Map<String, Object> config() {
        Map<String, Object> m = new HashMap<>();
        m.put("appTitle", "珠峰AI超级系统");
        m.put("registrationEnabled", true);
        m.put("emailLoginEnabled", true);
        m.put("socialLogins", List.of());
        m.put("ldap", Map.of("enabled", false));
        m.put("balance", Map.of("enabled", true, "autoRefillEnabled", true, "startBalance", 100000));
        m.put("conversationImportMaxFileSize", "20mb");
        return m;
    }

    @GetMapping("/api/user")
    public Map<String, Object> user() {
        User user = currentUser();
        return AuthController.toVo(user);
    }

    @GetMapping("/api/balance")
    public Map<String, Object> balance() {
        User user = currentUser();
        QueryWrapper<Balance> qw = new QueryWrapper<>();
        qw.eq("user_id", user.getId());
        Balance balance = balanceMapper.selectOne(qw);
        Map<String, Object> m = new HashMap<>();
        m.put("tokenCredits", balance == null ? 0 : balance.getTokenCredits());
        m.put("autoRefillEnabled", balance != null && Boolean.TRUE.equals(balance.getAutoRefillEnabled()));
        m.put("expireAt", balance == null ? null : balance.getExpireAt());
        m.put("user", user.getPublicId());
        return m;
    }

    @GetMapping("/api/notifications")
    public Map<String, Object> notifications() {
        return Map.of("items", List.of(), "total", 0, "preferences", Map.of("muted", false));
    }

    @GetMapping("/api/roles/user")
    public Map<String, Object> roles() {
        return Map.of("role", "USER", "name", "User",
                "permissions", Map.of("AGENTS", Map.of("CREATE", true, "SHARE", false, "SHARE_PUBLIC", false)));
    }

    @GetMapping("/api/models")
    public Map<String, Object> models() {
        return Map.of(
                "agents", List.of("珠峰默认模型"),
                "DeepSeek", List.of("deepseek-chat", "deepseek-reasoner")
        );
    }

    @GetMapping("/api/search/enable")
    public boolean searchEnabled() {
        return false;
    }

    @GetMapping("/api/endpoints")
    public Map<String, Object> endpoints() {
        return Map.of("agents", true);
    }

    @GetMapping("/api/banner")
    public Map<String, Object> banner() {
        return Map.of("show", false);
    }

    private User currentUser() {
        Long userId = UserContext.require();
        User user = userMapper.selectById(userId);
        if (user == null) throw new ApiException(401, "user not found");
        return user;
    }
}
