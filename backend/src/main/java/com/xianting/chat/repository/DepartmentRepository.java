package com.xianting.chat.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.xianting.chat.entity.Department;

import java.util.List;

/** 部门数据仓储接口，继承 MyBatis-Plus IRepository */
public interface DepartmentRepository extends IRepository<Department> {

    List<Department> listAll();

    /** 全部部门，按 sort_order 升序 */
    List<Department> listAllOrderBySortOrder();

    /** 按名称或 publicId 查找 */
    Department findByNameOrPublicId(String keyword);

}
