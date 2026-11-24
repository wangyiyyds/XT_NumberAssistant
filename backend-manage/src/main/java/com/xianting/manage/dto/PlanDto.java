package com.xianting.manage.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 套餐 DTO，departments 为该套餐开放的部门（跨表数据，由 Service 组装后经转换器注入） */
@Data
public class PlanDto {
    /** 对应实体 publicId */
    private String id;
    private String name;
    /** 套餐等级，数字越大功能越全 */
    private Integer level;
    private String description;
    private List<DepartmentDto> departments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
