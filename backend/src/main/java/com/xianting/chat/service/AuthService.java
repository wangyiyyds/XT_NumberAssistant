package com.xianting.chat.service;

import java.util.Map;

/** 登录/注册业务接口 */
public interface AuthService {

    /** 邮箱密码登录，返回 {token, user} */
    Map<String, Object> login(String email, String password);

    /** 注册并初始化余额，返回 {token, user} */
    Map<String, Object> register(String email, String password, String name, String brand);

    /** 换发新 token，返回 {token} */
    Map<String, Object> refresh(Long userId);
}
