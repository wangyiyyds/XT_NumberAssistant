package com.xianting.manage.dto;

import lombok.Data;

/** 给用户设置套餐的请求体：planId 为套餐 publicId，传 null 表示清除套餐 */
@Data
public class AssignPlanRequest {
    private String planId;
}
