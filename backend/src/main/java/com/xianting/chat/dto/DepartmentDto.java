package com.xianting.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/** 部门 DTO，对外字段与原接口一致 */
@Data
public class DepartmentDto {
    /** 对应实体 publicId */
    @JsonProperty("_id")
    private String id;
    private String name;
    private Boolean enabled;
    private Boolean brandOnly;
    private Integer sortOrder;
    /** 预留字段，恒为空列表 */
    private List<String> extraTools;
    /** 兼容旧接口的版本号字段，恒为 0 */
    @JsonProperty("__v")
    private Integer v;
}
