package com.xianting.manage.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 菜单 DTO：前端侧栏与管理页共用 */
@Data
public class MenuDto {
    /** 对应实体 publicId */
    private String id;
    private String name;
    private String path;
    /** @element-plus/icons-vue 导出名，前端解析成组件；未知名由前端回退默认图标 */
    private String icon;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
