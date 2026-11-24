package com.xianting.manage.service;

import com.xianting.manage.dto.AdminDto;
import com.xianting.manage.dto.AdminSaveRequest;

import java.util.List;

/** 管理员账号：列表与新增 */
public interface AdminService {

    List<AdminDto> list();

    AdminDto create(AdminSaveRequest request);
}
