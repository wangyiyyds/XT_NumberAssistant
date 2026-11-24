package com.xianting.manage.controller;

import com.xianting.manage.common.Result;
import com.xianting.manage.dto.MenuDto;
import com.xianting.manage.dto.MenuSaveRequest;
import com.xianting.manage.service.MenuService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 菜单管理：管理端左侧导航的数据源（/api/** 已由 AuthInterceptor 保护，需登录访问） */
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

    @GetMapping("/{publicId}")
    public Result<MenuDto> detail(@PathVariable String publicId) {
        return Result.ok(menuService.detail(publicId));
    }

    @PostMapping
    public Result<MenuDto> create(@RequestBody MenuSaveRequest request) {
        return Result.ok(menuService.create(request));
    }

    @PutMapping("/{publicId}")
    public Result<MenuDto> update(@PathVariable String publicId, @RequestBody MenuSaveRequest request) {
        return Result.ok(menuService.update(publicId, request));
    }

    @DeleteMapping("/{publicId}")
    public Result<Void> delete(@PathVariable String publicId) {
        menuService.delete(publicId);
        return Result.ok(null);
    }
}
