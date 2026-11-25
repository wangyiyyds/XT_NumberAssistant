package com.xianting.manage.controller;

import com.xianting.manage.common.Result;
import com.xianting.manage.dto.MenuDto;
import com.xianting.manage.service.MenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单读取：前端侧栏登录后取这份数据渲染。
 * 不提供增删改——菜单项对应编译期写死的前端路由，见 MenuService 注释。
 */
@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public Result<List<MenuDto>> list() {
        return Result.ok(menuService.list());
    }
}
