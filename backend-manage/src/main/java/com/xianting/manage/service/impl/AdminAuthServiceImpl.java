package com.xianting.manage.service.impl;

import com.xianting.manage.common.ApiException;
import com.xianting.manage.common.PasswordUtil;
import com.xianting.manage.common.TokenUtil;
import com.xianting.manage.converter.AdminConverter;
import com.xianting.manage.dto.AdminDto;
import com.xianting.manage.entity.Admin;
import com.xianting.manage.repository.AdminRepository;
import com.xianting.manage.service.AdminAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminRepository adminRepository;
    private final AdminConverter adminConverter;

    @Value("${app.jwt.secret}")
    private String secret;
    @Value("${app.jwt.expire-hours}")
    private long expireHours;

    public AdminAuthServiceImpl(AdminRepository adminRepository, AdminConverter adminConverter) {
        this.adminRepository = adminRepository;
        this.adminConverter = adminConverter;
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        if (username == null || password == null) {
            throw new ApiException(400, "username and password are required");
        }
        Admin admin = adminRepository.findByUsername(username.trim());
        if (admin == null || !PasswordUtil.matches(password, admin.getPassword())) {
            throw new ApiException(401, "用户名或密码错误");
        }
        if (Boolean.TRUE.equals(admin.getDisabled())) {
            throw new ApiException(403, "账号已禁用");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", TokenUtil.issue(admin.getId(), secret, expireHours));
        m.put("admin", adminConverter.toDto(admin));
        return m;
    }

    @Override
    public AdminDto me(Long adminId) {
        return adminConverter.toDto(requireById(adminId));
    }

    @Override
    public String usernameOf(Long adminId) {
        return requireById(adminId).getUsername();
    }

    private Admin requireById(Long adminId) {
        Admin admin = adminRepository.getById(adminId);
        if (admin == null) throw new ApiException(401, "unauthorized");
        return admin;
    }
}
