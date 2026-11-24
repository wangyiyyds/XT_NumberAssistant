package com.xianting.manage.dto;

import lombok.Data;

/** 部门 DTO，与客户端 DepartmentDto 同形 */
@Data
public class DepartmentDto {
    /** 对应实体 publicId */
    private String id;
    private String name;
    private Boolean enabled;
    private Boolean brandOnly;
    private Integer sortOrder;
}
