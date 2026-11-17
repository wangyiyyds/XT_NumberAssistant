package com.xianting.chat.controller;

import com.xianting.chat.common.UserContext;
import com.xianting.chat.dto.BalanceDto;
import com.xianting.chat.dto.UserDto;
import com.xianting.chat.service.BalanceService;
import com.xianting.chat.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Miscellaneous bootstrap endpoints observed on the target site. */
@RestController
public class CoreController {

    private final UserService userService;
    private final BalanceService balanceService;

    public CoreController(UserService userService, BalanceService balanceService) {
        this.userService = userService;
        this.balanceService = balanceService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/api/config")
    public Map<String, Object> config() {
        Map<String, Object> m = new HashMap<>();
        m.put("appTitle", "先t霆AI超级系统");
        m.put("registrationEnabled", true);
        m.put("emailLoginEnabled", true);
        m.put("socialLogins", List.of());
        m.put("ldap", Map.of("enabled", false));
        m.put("balance", Map.of("enabled", true, "autoRefillEnabled", true, "startBalance", 100000));
        m.put("conversationImportMaxFileSize", "20mb");
        return m;
    }

    @GetMapping("/api/user")
    public UserDto user() {
        return userService.getUser(UserContext.require());
    }

    @GetMapping("/api/balance")
    public BalanceDto balance() {
        return balanceService.getBalance(UserContext.require());
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
                "agents", List.of("先霆默认模型"),
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
}
