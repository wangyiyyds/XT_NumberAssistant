package com.xianting.manage.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.xianting.manage.entity.User;
import com.xianting.manage.mapper.UserMapper;
import com.xianting.manage.repository.UserRepository;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl extends CrudRepository<UserMapper, User> implements UserRepository {

    @Override
    public Page<User> pageByKeyword(long current, long size, String keyword, Long planId) {
        //  WHERE (email LIKE ? OR name LIKE ?) AND plan_id = ?，按注册时间倒序
        QueryWrapper<User> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim() + "%";
            qw.and(w -> w.like("email", like).or().like("name", like));
        }
        if (planId != null) {
            qw.eq("plan_id", planId);
        }
        qw.orderByDesc("created_at");
        return getBaseMapper().selectPage(new Page<>(current, size), qw);
    }

    @Override
    public User findByPublicId(String publicId) {
        //  WHERE public_id = {publicId}
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("public_id", publicId);
        return getBaseMapper().selectOne(qw);
    }

    @Override
    public long countByPlanId(Long planId) {
        //  WHERE plan_id = {planId}
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("plan_id", planId);
        return getBaseMapper().selectCount(qw);
    }

    @Override
    public boolean existsByEmail(String email) {
        //  WHERE email = {email}
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("email", email);
        return getBaseMapper().selectCount(qw) > 0;
    }
}
