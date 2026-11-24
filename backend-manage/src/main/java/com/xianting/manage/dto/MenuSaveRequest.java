package com.xianting.manage.dto;

import lombok.Data;

/** 菜单新建/编辑请求体 */
@Data
public class MenuSaveRequest {
    private String name;
    /** 前端路由路径，须以 / 开头，全表唯一 */
    private String path;
    private String icon;
    private Integer sortOrder;
}
