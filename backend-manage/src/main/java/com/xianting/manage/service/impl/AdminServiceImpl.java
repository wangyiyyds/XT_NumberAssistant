package com.xianting.manage.service.impl;

import com.xianting.manage.common.ApiException;
import com.xianting.manage.common.PasswordUtil;
import com.xianting.manage.converter.AdminConverter;
import com.xianting.manage.dto.AdminDto;
import com.xianting.manage.dto.AdminSaveRequest;
import com.xianting.manage.entity.Admin;
import com.xianting.manage.repository.AdminRepository;
import com.xianting.manage.service.AdminService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final AdminConverter adminConverter;

    public AdminServiceImpl(AdminRepository adminRepository, AdminConverter adminConverter) {
        this.adminRepository = adminRepository;
        this.adminConverter = adminConverter;
    }

    @Override
    public List<AdminDto> list() {
        return adminRepository.listAllOrderById().stream()
                .map(adminConverter::toDto)
                .toList();
    }

    @Override
    public AdminDto create(AdminSaveRequest request) {
        validate(request);
        String username = request.getUsername().trim();
        if (adminRepository.findByUsername(username) != null) {
            throw new ApiException(409, "账号 " + username + " 已存在");
        }
        Admin admin = new Admin();
        admin.setPublicId(UUID.randomUUID().toString());
        admin.setUsername(username);
        admin.setPassword(PasswordUtil.hash(request.getPassword()));
        admin.setName(request.getName() == null ? null : request.getName().trim());
        admin.setDisabled(false);
        adminRepository.save(admin);
        return adminConverter.toDto(admin);
    }

    private void validate(AdminSaveRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ApiException(400, "登录账号必填");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new ApiException(400, "密码必填");
        }
    }
}
