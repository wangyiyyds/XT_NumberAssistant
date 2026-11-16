package com.zhufeng.chat.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.zhufeng.chat.entity.User;

/** 用户数据仓储接口，继承 MyBatis-Plus IRepository */
public interface UserRepository extends IRepository<User> {


    User findByEmail(String email);

    boolean existsByEmail(String email);

}
