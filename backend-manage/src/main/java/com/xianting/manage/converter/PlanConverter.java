package com.xianting.manage.converter;

import com.xianting.manage.dto.DepartmentDto;
import com.xianting.manage.dto.PlanDto;
import com.xianting.manage.dto.PlanSaveRequest;
import com.xianting.manage.entity.Plan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/** Plan 实体 <-> PlanDto 转换器 */
@Mapper(componentModel = "spring")
public interface PlanConverter {

    /** 开放的部门列表为跨表数据，由 Service 组装后经第二个入参注入 */
    @Mapping(target = "id", source = "plan.publicId")
    @Mapping(target = "departments", source = "departments")
    PlanDto toDto(Plan plan, List<DepartmentDto> departments);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Plan toEntity(PlanSaveRequest request);
}
