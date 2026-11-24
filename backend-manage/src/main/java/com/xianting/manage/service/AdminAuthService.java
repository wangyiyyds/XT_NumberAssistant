package com.xianting.manage.service;

import com.xianting.manage.dto.AdminDto;

import java.util.Map;

public interface AdminAuthService {

    /** 管理员登录：返回 {token, admin} */
    Map<String, Object> login(String username, String password);

    /** 当前登录管理员信息 */
    AdminDto me(Long adminId);

    /** 取管理员用户名（提示词 createdBy/updatedBy 记录用） */
    String usernameOf(Long adminId);
}
