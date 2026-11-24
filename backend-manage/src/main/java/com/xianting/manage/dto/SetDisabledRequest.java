package com.xianting.manage.dto;

import lombok.Data;

/** 禁用/启用用户的请求体 */
@Data
public class SetDisabledRequest {
    private Boolean disabled;
}
