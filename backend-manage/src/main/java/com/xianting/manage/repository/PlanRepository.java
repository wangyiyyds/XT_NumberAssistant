package com.xianting.manage.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.xianting.manage.entity.Plan;

import java.util.List;

/** 套餐数据仓储接口，继承 MyBatis-Plus IRepository */
public interface PlanRepository extends IRepository<Plan> {

    /** 全部套餐，按等级升序 */
    List<Plan> listAllOrderByLevel();

    Plan findByPublicId(String publicId);

    /** 等级唯一性预检（uk_plan_level 兜底） */
    long countByLevel(Integer level);
}
