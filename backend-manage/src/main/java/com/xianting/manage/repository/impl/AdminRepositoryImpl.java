package com.xianting.manage.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.xianting.manage.entity.Admin;
import com.xianting.manage.mapper.AdminMapper;
import com.xianting.manage.repository.AdminRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AdminRepositoryImpl extends CrudRepository<AdminMapper, Admin> implements AdminRepository {

    @Override
    public Admin findByUsername(String username) {
        //  WHERE username = {username}
        QueryWrapper<Admin> qw = new QueryWrapper<>();
        qw.eq("username", username);
        return getBaseMapper().selectOne(qw);
    }

    @Override
    public List<Admin> listAllOrderById() {
        //  ORDER BY id ASC
        QueryWrapper<Admin> qw = new QueryWrapper<>();
        qw.orderByAsc("id");
        return getBaseMapper().selectList(qw);
    }
}
