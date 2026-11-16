package com.zhufeng.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.zhufeng.chat.entity.User;
import com.zhufeng.chat.mapper.UserMapper;
import com.zhufeng.chat.repository.UserRepository;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl extends CrudRepository<UserMapper, User> implements UserRepository {

    @Override
    public User findByEmail(String email) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("email", email);
        return getBaseMapper().selectOne(qw);
    }

    @Override
    public boolean existsByEmail(String email) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("email", email);
        return getBaseMapper().selectCount(qw) > 0;
    }

}
