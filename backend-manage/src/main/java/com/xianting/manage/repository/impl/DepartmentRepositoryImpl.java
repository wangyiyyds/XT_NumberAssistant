package com.xianting.manage.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.xianting.manage.entity.Department;
import com.xianting.manage.mapper.DepartmentMapper;
import com.xianting.manage.repository.DepartmentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DepartmentRepositoryImpl extends CrudRepository<DepartmentMapper, Department> implements DepartmentRepository {

    @Override
    public List<Department> listAllOrderBySortOrder() {
        //  ORDER BY sort_order ASC
        QueryWrapper<Department> qw = new QueryWrapper<>();
        qw.orderByAsc("sort_order");
        return getBaseMapper().selectList(qw);
    }

    @Override
    public Department findByPublicId(String publicId) {
        //  WHERE public_id = {publicId}
        QueryWrapper<Department> qw = new QueryWrapper<>();
        qw.eq("public_id", publicId);
        return getBaseMapper().selectOne(qw);
    }
}
