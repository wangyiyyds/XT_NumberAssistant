package com.xianting.manage.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 给用户设置套餐的请求体：planId 为套餐 publicId，传 null 表示清除套餐 */
@Data
public class AssignPlanRequest {

    /** 可空（空=清除套餐）；非空时存在性由 Service 校验 */
    @Size(max = 64, message = "套餐标识长度不能超过64")
    private String planId;
}
