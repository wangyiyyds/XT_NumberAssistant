package com.xianting.manage.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.xianting.manage.entity.Menu;
import com.xianting.manage.mapper.MenuMapper;
import com.xianting.manage.repository.MenuRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MenuRepositoryImpl extends CrudRepository<MenuMapper, Menu> implements MenuRepository {

    @Override
    public List<Menu> listAllOrderBySortOrder() {
        //  ORDER BY sort_order ASC
        QueryWrapper<Menu> qw = new QueryWrapper<>();
        qw.orderByAsc("sort_order");
        return getBaseMapper().selectList(qw);
    }

    @Override
    public Menu findByPublicId(String publicId) {
        //  WHERE public_id = {publicId}
        QueryWrapper<Menu> qw = new QueryWrapper<>();
        qw.eq("public_id", publicId);
        return getBaseMapper().selectOne(qw);
    }

    @Override
    public long countByPath(String path) {
        //  WHERE path = {path}
        QueryWrapper<Menu> qw = new QueryWrapper<>();
        qw.eq("path", path);
        return getBaseMapper().selectCount(qw);
    }
}
