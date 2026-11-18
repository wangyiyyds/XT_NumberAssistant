package com.xianting.chat.controller;

import com.xianting.chat.common.Result;
import com.xianting.chat.common.UserContext;
import com.xianting.chat.service.AuthService;
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
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return Result.ok(authService.login(body.get("email"), body.get("password")));
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        return Result.ok(authService.register(body.get("email"), body.get("password"),
                body.getOrDefault("name", "新用户"), body.getOrDefault("brand", "xianting")));
    }

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh() {
        return Result.ok(authService.refresh(UserContext.require()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok(null);
    }
}
