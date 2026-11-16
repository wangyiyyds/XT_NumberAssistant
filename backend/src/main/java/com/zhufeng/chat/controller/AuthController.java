package com.zhufeng.chat.controller;

import com.zhufeng.chat.common.UserContext;
import com.zhufeng.chat.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        return authService.login(body.get("email"), body.get("password"));
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        return authService.register(body.get("email"), body.get("password"),
                body.getOrDefault("name", "新用户"), body.getOrDefault("brand", "zhufeng"));
    }

    @PostMapping("/refresh")
    public Map<String, Object> refresh() {
        return authService.refresh(UserContext.require());
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        return Map.of("success", true);
    }
}
