package com.xianting.manage.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 禁用/启用用户的请求体 */
@Data
public class SetDisabledRequest {

    /** 不接受 null：禁用与启用语义相反，缺省无合理默认值 */
    @NotNull(message = "disabled 必填")
    private Boolean disabled;
}
