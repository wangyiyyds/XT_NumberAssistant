package com.xianting.manage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 菜单新建/编辑请求体 */
@Data
public class MenuSaveRequest {

    /** 对应 t_menu.name VARCHAR(64) NOT NULL */
    @NotBlank(message = "菜单名称必填")
    @Size(max = 64, message = "菜单名称长度不能超过64")
    private String name;

    /** 前端路由路径，须以 / 开头，全表唯一（唯一性由 Service 校验） */
    @NotBlank(message = "路由路径必填")
    @Pattern(regexp = "^/.*$", message = "路由路径须以 / 开头")
    @Size(max = 128, message = "路由路径长度不能超过128")
    private String path;

    /** @element-plus/icons-vue 导出名，对应 t_menu.icon VARCHAR(64) */
    @Size(max = 64, message = "图标名长度不能超过64")
    private String icon;

    /** 对应 t_menu.sort_order INT NOT NULL */
    @NotNull(message = "排序不能为空")
    private Integer sortOrder;
}
