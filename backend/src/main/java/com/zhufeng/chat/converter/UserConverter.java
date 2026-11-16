package com.zhufeng.chat.converter;

import com.zhufeng.chat.dto.UserDto;
import com.zhufeng.chat.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** User 实体 <-> UserDto 转换器（MapStruct 编译期生成实现） */
@Mapper(componentModel = "spring")
public interface UserConverter {

    @Mapping(target = "id", source = "publicId")
    UserDto toDto(User user);

    @Mapping(target = "publicId", source = "id")
    @Mapping(target = "id", ignore = true)
    /** 密码哈希属于服务端敏感字段，不参与 DTO 转换 */
    @Mapping(target = "password", ignore = true)
    User toEntity(UserDto dto);
}
