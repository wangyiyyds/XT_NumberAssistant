package com.zhufeng.chat.converter;

import com.zhufeng.chat.dto.InspirationVideoDto;
import com.zhufeng.chat.entity.InspirationVideo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** InspirationVideo 实体 <-> InspirationVideoDto 转换器 */
@Mapper(componentModel = "spring")
public interface InspirationVideoConverter {

    @Mapping(target = "lowFansHighLikes", expression = "java(Boolean.TRUE.equals(video.getLowFansHighLikes()))")
    InspirationVideoDto toDto(InspirationVideo video);

    InspirationVideo toEntity(InspirationVideoDto dto);
}
