package com.zhufeng.chat.service.impl;

import com.zhufeng.chat.common.ApiException;
import com.zhufeng.chat.converter.UserConverter;
import com.zhufeng.chat.dto.UserDto;
import com.zhufeng.chat.entity.User;
import com.zhufeng.chat.repository.UserRepository;
import com.zhufeng.chat.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserConverter userConverter;

    public UserServiceImpl(UserRepository userRepository, UserConverter userConverter) {
        this.userRepository = userRepository;
        this.userConverter = userConverter;
    }

    @Override
    public User requireUser(Long userId) {
        User user = userRepository.getById(userId);
        if (user == null) throw new ApiException(401, "user not found");
        return user;
    }

    @Override
    public UserDto getUser(Long userId) {
        return userConverter.toDto(requireUser(userId));
    }
}
