package com.xianting.manage.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.xianting.manage.entity.Menu;

import java.util.List;

/** 菜单数据仓储接口，继承 MyBatis-Plus IRepository */
public interface MenuRepository extends IRepository<Menu> {

    /** 全部菜单，按 sortOrder 升序 */
    List<Menu> listAllOrderBySortOrder();
}
