package com.xianting.manage.converter;

import com.xianting.manage.dto.AdminDto;
import com.xianting.manage.entity.Admin;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Admin 实体 -> AdminDto 转换器（password 不进 DTO） */
@Mapper(componentModel = "spring")
public interface AdminConverter {

    @Mapping(target = "id", source = "publicId")
    AdminDto toDto(Admin admin);
}
