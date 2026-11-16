package com.zhufeng.chat.service;

import com.zhufeng.chat.dto.ConversationDto;
import com.zhufeng.chat.dto.MessageDto;

import java.util.List;
import java.util.Map;

/** 会话业务接口 */
public interface ConversationService {

    /** 会话列表，agentPublicId 可为空 */
    Map<String, Object> list(Long userId, String agentPublicId);

    /** 会话消息列表 */
    List<MessageDto> messages(Long userId, String publicId);

    /** 重命名会话 */
    ConversationDto rename(Long userId, String publicId, String title);

    /** 删除会话及其全部消息 */
    Map<String, Object> delete(Long userId, String publicId);
}
