package com.xianting.manage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 套餐-部门多对多关系（纯内部关联，不对外暴露 publicId） */
@Data
@TableName("t_plan_department")
public class PlanDepartment {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** FK 逻辑关联 -> t_plan.id */
    private Long planId;
    /** FK 逻辑关联 -> t_department.id */
    private Long departmentId;
}
