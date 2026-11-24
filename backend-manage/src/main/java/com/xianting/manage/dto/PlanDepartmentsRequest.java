package com.xianting.manage.dto;

import lombok.Data;

import java.util.List;

/** 配置套餐开放部门的请求体：departmentIds 为部门 publicId 列表，全量替换语义 */
@Data
public class PlanDepartmentsRequest {
    private List<String> departmentIds;
}
