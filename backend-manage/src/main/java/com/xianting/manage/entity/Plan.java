package com.xianting.manage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 套餐：等级(level)决定用户可访问哪些部门(功能)，关系在 t_plan_department */
@Data
@TableName("t_plan")
public class Plan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String publicId;
    private String name;
    /** 套餐等级，数字越大功能越全，全表唯一 */
    private Integer level;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
