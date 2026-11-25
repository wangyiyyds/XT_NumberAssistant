package com.xianting.manage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 部门新建/编辑请求体（启停=enabled，排序=sortOrder，均由 PUT 承载） */
@Data
public class DepartmentSaveRequest {

    /** 对应 t_department.name VARCHAR(64) NOT NULL */
    @NotBlank(message = "部门名称必填")
    @Size(max = 64, message = "部门名称长度不能超过64")
    private String name;

    private Boolean enabled;
    private Boolean brandOnly;
    private Integer sortOrder;
}
