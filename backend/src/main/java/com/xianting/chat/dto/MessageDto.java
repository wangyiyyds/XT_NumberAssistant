package com.xianting.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/** 消息 DTO，对外字段与原接口一致 */
@Data
public class MessageDto {
    /** 对应实体 publicId */
    @JsonProperty("_id")
    private String id;
    private String messageId;
    /** 会话 publicId（跨表数据，由转换器第二个入参提供） */
    private String conversationId;
    private String parentMessageId;
    private String sender;
    /** 由 sender 计算得出 */
    @JsonProperty("isCreatedByUser")
    private boolean createdByUser;
    private String text;
    private boolean error;
    private LocalDateTime createdAt;
}
