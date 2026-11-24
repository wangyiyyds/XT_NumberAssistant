package com.xianting.manage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 部门（即客户端左侧导航的功能分组），与套餐的关系在 t_plan_department */
@Data
@TableName("t_department")
public class Department {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String publicId;
    private String name;
    private Boolean enabled;
    private Boolean brandOnly;
    private Integer sortOrder;
}
