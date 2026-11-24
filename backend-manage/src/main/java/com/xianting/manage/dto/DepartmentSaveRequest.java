package com.xianting.manage.dto;

import lombok.Data;

/** 部门新建/编辑请求体（启停=enabled，排序=sortOrder，均由 PUT 承载） */
@Data
public class DepartmentSaveRequest {
    private String name;
    private Boolean enabled;
    private Boolean brandOnly;
    private Integer sortOrder;
}
