package com.xianting.chat.converter;

import com.xianting.chat.dto.DepartmentDto;
import com.xianting.chat.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Department 实体 <-> DepartmentDto 转换器 */
@Mapper(componentModel = "spring")
public interface DepartmentConverter {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "extraTools", ignore = true)
    @Mapping(target = "v", constant = "0")
    DepartmentDto toDto(Department department);

    @Mapping(target = "publicId", source = "id")
    @Mapping(target = "id", ignore = true)
    Department toEntity(DepartmentDto dto);
}
