package com.xianting.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.xianting.chat.entity.Department;
import com.xianting.chat.mapper.DepartmentMapper;
import com.xianting.chat.repository.DepartmentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DepartmentRepositoryImpl extends CrudRepository<DepartmentMapper, Department> implements DepartmentRepository {

    @Override
    public List<Department> listAll() {
        return getBaseMapper().selectList(null);
    }

    @Override
    public List<Department> listAllOrderBySortOrder() {
        QueryWrapper<Department> qw = new QueryWrapper<>();
        qw.orderByAsc("sort_order");
        return getBaseMapper().selectList(qw);
    }

    @Override
    public Department findByNameOrPublicId(String keyword) {
        QueryWrapper<Department> qw = new QueryWrapper<>();
        qw.eq("name", keyword).or().eq("public_id", keyword);
        return getBaseMapper().selectOne(qw);
    }

}
