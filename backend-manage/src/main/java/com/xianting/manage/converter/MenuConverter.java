package com.xianting.manage.converter;

import com.xianting.manage.dto.MenuDto;
import com.xianting.manage.entity.Menu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Menu 实体 -> MenuDto 转换器（菜单只读，无反向映射） */
@Mapper(componentModel = "spring")
public interface MenuConverter {

    @Mapping(target = "id", source = "publicId")
    MenuDto toDto(Menu menu);
}
