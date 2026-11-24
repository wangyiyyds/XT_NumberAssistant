package com.xianting.manage.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.xianting.manage.entity.Department;

import java.util.List;

/** 部门数据仓储接口，继承 MyBatis-Plus IRepository */
public interface DepartmentRepository extends IRepository<Department> {

    List<Department> listAllOrderBySortOrder();

    Department findByPublicId(String publicId);
}
