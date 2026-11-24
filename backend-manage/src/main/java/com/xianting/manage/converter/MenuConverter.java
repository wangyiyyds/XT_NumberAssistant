package com.xianting.manage.converter;

import com.xianting.manage.dto.MenuDto;
import com.xianting.manage.dto.MenuSaveRequest;
import com.xianting.manage.entity.Menu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Menu 实体 <-> MenuDto 转换器 */
@Mapper(componentModel = "spring")
public interface MenuConverter {

    @Mapping(target = "id", source = "publicId")
    MenuDto toDto(Menu menu);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Menu toEntity(MenuSaveRequest request);
}
