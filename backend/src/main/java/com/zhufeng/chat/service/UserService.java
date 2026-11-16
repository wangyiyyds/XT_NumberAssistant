package com.zhufeng.chat.service;

import com.zhufeng.chat.dto.UserDto;
import com.zhufeng.chat.entity.User;

/** 用户信息业务接口 */
public interface UserService {

    /** 按 id 查用户，不存在抛 401 */
    User requireUser(Long userId);

    /** 当前用户信息 DTO */
    UserDto getUser(Long userId);
}
