package com.xianting.chat.converter;

import com.xianting.chat.dto.MessageDto;
import com.xianting.chat.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Message 实体 <-> MessageDto 转换器 */
@Mapper(componentModel = "spring")
public interface MessageConverter {

    @Mapping(target = "id", source = "message.publicId")
    @Mapping(target = "messageId", source = "message.publicId")
    @Mapping(target = "conversationId", source = "conversationPublicId")
    @Mapping(target = "createdByUser", expression = "java(\"user\".equals(message.getSender()))")
    @Mapping(target = "error", expression = "java(Boolean.TRUE.equals(message.getError()))")
    MessageDto toDto(Message message, String conversationPublicId);

    @Mapping(target = "publicId", source = "dto.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conversationId", ignore = true)
    Message toEntity(MessageDto dto);
}
