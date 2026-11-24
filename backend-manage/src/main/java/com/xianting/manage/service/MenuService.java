package com.xianting.manage.service;

import com.xianting.manage.dto.MenuDto;
import com.xianting.manage.dto.MenuSaveRequest;

import java.util.List;

/** 管理端左侧导航菜单：查询与 CRUD */
public interface MenuService {

    List<MenuDto> list();

    MenuDto detail(String publicId);

    MenuDto create(MenuSaveRequest request);

    MenuDto update(String publicId, MenuSaveRequest request);

    void delete(String publicId);
}
