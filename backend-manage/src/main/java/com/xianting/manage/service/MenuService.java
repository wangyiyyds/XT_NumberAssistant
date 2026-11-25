package com.xianting.manage.service;

import com.xianting.manage.dto.MenuDto;

import java.util.List;

/**
 * 管理端左侧导航菜单：只读。
 *
 * 菜单项即前端路由，页面组件在编译期就固定在 bundle 里，数据库只决定侧栏渲染哪些链接、
 * 叫什么名、用什么图标、排第几。新增菜单必须同时改前端路由，因此不提供增删改接口，
 * 菜单内容以 schema-manage.sql 的种子数据为唯一事实源。
 */
public interface MenuService {

    List<MenuDto> list();
}
