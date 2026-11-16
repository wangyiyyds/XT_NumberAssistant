package com.zhufeng.chat.converter;

import com.zhufeng.chat.dto.InspirationKeywordDto;
import com.zhufeng.chat.entity.InspirationKeyword;
import com.zhufeng.chat.entity.InspirationVideo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/** InspirationKeyword 实体 <-> InspirationKeywordDto 转换器，视频列表借助 InspirationVideoConverter */
@Mapper(componentModel = "spring", uses = InspirationVideoConverter.class)
public interface InspirationKeywordConverter {

    @Mapping(target = "id", source = "keyword.id")
    @Mapping(target = "order", source = "keyword.sortOrder")
    @Mapping(target = "videos", source = "videos")
    InspirationKeywordDto toDto(InspirationKeyword keyword, List<InspirationVideo> videos);

    @Mapping(target = "sortOrder", source = "order")
    @Mapping(target = "userId", ignore = true)
    InspirationKeyword toEntity(InspirationKeywordDto dto);
}
