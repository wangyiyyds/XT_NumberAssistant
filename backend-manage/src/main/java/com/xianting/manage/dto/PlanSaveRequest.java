package com.xianting.manage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 套餐新建/编辑请求体 */
@Data
public class PlanSaveRequest {

    /** 对应 t_plan.name VARCHAR(64) NOT NULL */
    @NotBlank(message = "套餐名称必填")
    @Size(max = 64, message = "套餐名称长度不能超过64")
    private String name;

    /** 套餐等级，数字越大功能越全；唯一性由 Service 校验（撞 uk_plan_level 返回 409） */
    @NotNull(message = "套餐等级必填")
    private Integer level;

    /** 对应 t_plan.description VARCHAR(512) */
    @Size(max = 512, message = "套餐描述长度不能超过512")
    private String description;
}
