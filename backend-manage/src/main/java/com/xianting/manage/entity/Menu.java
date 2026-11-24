package com.xianting.manage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 菜单项：管理端左侧导航，存于 t_menu */
@Data
@TableName("t_menu")
public class Menu {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String publicId;
    /** 菜单显示名称 */
    private String name;
    /** 前端路由路径，全表唯一 */
    private String path;
    /** @element-plus/icons-vue 导出名（如 Cpu），前端解析成组件 */
    private String icon;
    /** 显示顺序，升序 */
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
