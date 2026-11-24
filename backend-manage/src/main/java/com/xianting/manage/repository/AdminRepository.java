package com.xianting.manage.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.xianting.manage.entity.Admin;

import java.util.List;

/** 管理员数据仓储接口，继承 MyBatis-Plus IRepository */
public interface AdminRepository extends IRepository<Admin> {

    Admin findByUsername(String username);

    /** 全部管理员，按创建先后（自增 id）升序 */
    List<Admin> listAllOrderById();
}
