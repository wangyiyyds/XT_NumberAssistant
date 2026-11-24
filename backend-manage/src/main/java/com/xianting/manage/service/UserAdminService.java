package com.xianting.manage.service;

import com.xianting.manage.common.PageResult;
import com.xianting.manage.dto.CreateUserRequest;
import com.xianting.manage.dto.UserAdminDto;

public interface UserAdminService {

    /** 管理端新增用户：同时初始化积分记录，逻辑与客户端注册一致 */
    UserAdminDto create(CreateUserRequest request);

    /**
     * 分页查用户（含套餐名与积分余额）。
     *
     * @param keyword       email/name 模糊关键字，可空
     * @param planPublicId  按套餐过滤，可空
     */
    PageResult<UserAdminDto> page(long page, long size, String keyword, String planPublicId);

    /** 给用户设置套餐；planPublicId 为 null 表示清除套餐 */
    UserAdminDto assignPlan(String userPublicId, String planPublicId);

    /** 禁用/启用用户（禁用后客户端登录被 403 拦截） */
    UserAdminDto setDisabled(String userPublicId, boolean disabled);
}
