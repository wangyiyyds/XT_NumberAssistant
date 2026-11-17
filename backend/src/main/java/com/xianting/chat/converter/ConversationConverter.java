package com.xianting.chat.converter;

import com.xianting.chat.dto.ConversationDto;
import com.xianting.chat.entity.Conversation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Conversation 实体 <-> ConversationDto 转换器 */
@Mapper(componentModel = "spring")
public interface ConversationConverter {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "conversationId", source = "publicId")
    /** 实体 agentId 是外键 Long，DTO agentId 是智能体 publicId，语义不同，由调用方按需 set */
    @Mapping(target = "agentId", ignore = true)
    ConversationDto toDto(Conversation conversation);

    @Mapping(target = "publicId", source = "id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agentId", ignore = true)
    Conversation toEntity(ConversationDto dto);
}
