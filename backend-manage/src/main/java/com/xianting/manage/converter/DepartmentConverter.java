package com.xianting.manage.converter;

import com.xianting.manage.dto.DepartmentDto;
import com.xianting.manage.dto.DepartmentSaveRequest;
import com.xianting.manage.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Department 实体 <-> DepartmentDto 转换器 */
@Mapper(componentModel = "spring")
public interface DepartmentConverter {

    @Mapping(target = "id", source = "publicId")
    DepartmentDto toDto(Department department);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    Department toEntity(DepartmentSaveRequest request);
}
