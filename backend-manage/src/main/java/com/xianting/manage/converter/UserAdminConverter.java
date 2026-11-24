package com.xianting.manage.converter;

import com.xianting.manage.dto.UserAdminDto;
import com.xianting.manage.entity.Balance;
import com.xianting.manage.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** User 实体 -> UserAdminDto 转换器：积分与套餐为跨表数据，由额外入参注入 */
@Mapper(componentModel = "spring")
public interface UserAdminConverter {

    @Mapping(target = "id", source = "user.publicId")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "avatar", source = "user.avatar")
    @Mapping(target = "emailVerified", source = "user.emailVerified")
    @Mapping(target = "disabled", source = "user.disabled")
    @Mapping(target = "createdAt", source = "user.createdAt")
    @Mapping(target = "planId", source = "planPublicId")
    @Mapping(target = "planName", source = "planName")
    @Mapping(target = "tokenCredits", source = "balance.tokenCredits")
    @Mapping(target = "autoRefillEnabled", source = "balance.autoRefillEnabled")
    @Mapping(target = "expireAt", source = "balance.expireAt")
    UserAdminDto toDto(User user, Balance balance, String planPublicId, String planName);
}
