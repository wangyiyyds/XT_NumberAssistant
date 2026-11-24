package com.xianting.manage.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.xianting.manage.entity.User;

/** 客户端用户数据仓储接口（管理端视角），继承 MyBatis-Plus IRepository */
public interface UserRepository extends IRepository<User> {

    /**
     * 分页查用户。
     *
     * @param keyword  匹配 email/name 的 LIKE 关键字，可空
     * @param planId   套餐主键过滤，可空
     */
    Page<User> pageByKeyword(long current, long size, String keyword, Long planId);

    User findByPublicId(String publicId);

    /** 删除套餐前检查是否仍有用户在用 */
    long countByPlanId(Long planId);

    /** 新增用户前查重邮箱 */
    boolean existsByEmail(String email);
}
