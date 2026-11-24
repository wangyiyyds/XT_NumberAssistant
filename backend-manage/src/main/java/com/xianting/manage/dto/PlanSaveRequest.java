package com.xianting.manage.dto;

import lombok.Data;

/** 套餐新建/编辑请求体 */
@Data
public class PlanSaveRequest {
    private String name;
    private Integer level;
    private String description;
}
