package com.xianting.chat.service.impl;

import com.xianting.chat.common.ApiException;
import com.xianting.chat.converter.ConversationConverter;
import com.xianting.chat.converter.MessageConverter;
import com.xianting.chat.dto.ConversationDto;
import com.xianting.chat.dto.MessageDto;
import com.xianting.chat.entity.Conversation;
import com.xianting.chat.entity.Message;
import com.xianting.chat.repository.ConversationRepository;
import com.xianting.chat.repository.MessageRepository;
import com.xianting.chat.service.AgentService;
import com.xianting.chat.service.ConversationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AgentService agentService;
    private final ConversationConverter conversationConverter;
    private final MessageConverter messageConverter;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   MessageRepository messageRepository,
                                   AgentService agentService,
                                   ConversationConverter conversationConverter,
                                   MessageConverter messageConverter) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.agentService = agentService;
        this.conversationConverter = conversationConverter;
        this.messageConverter = messageConverter;
    }

    @Override
    public Map<String, Object> list(Long userId, String agentPublicId) {
        Long agentId = null;
        if (agentPublicId != null && !agentPublicId.isBlank()) {
            agentId = agentService.requireByPublicId(agentPublicId).getId();
        }
        List<ConversationDto> convos = new ArrayList<>();
        for (Conversation c : conversationRepository.listByUser(userId, agentId)) {
            convos.add(conversationConverter.toDto(c));
        }
        return Map.of("conversations", convos);
    }

    @Override
    public List<MessageDto> messages(Long userId, String publicId) {
        Conversation c = requireOwn(publicId, userId);
        List<MessageDto> out = new ArrayList<>();
        for (Message m : messageRepository.listByConversationId(c.getId())) {
            out.add(messageConverter.toDto(m, c.getPublicId()));
        }
        return out;
    }

    @Override
    public ConversationDto rename(Long userId, String publicId, String title) {
        Conversation c = requireOwn(publicId, userId);
        if (title == null || title.isBlank()) throw new ApiException(400, "title is required");
        c.setTitle(title.trim());
        c.setUpdatedAt(LocalDateTime.now());
        conversationRepository.updateById(c);
        return conversationConverter.toDto(c);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> delete(Long userId, String publicId) {
        Conversation c = requireOwn(publicId, userId);
        messageRepository.deleteByConversationId(c.getId());
        conversationRepository.removeById(c.getId());
        return Map.of("success", true);
    }

    private Conversation requireOwn(String publicId, Long userId) {
        Conversation c = conversationRepository.findByPublicIdAndUserId(publicId, userId);
        if (c == null) throw new ApiException(404, "conversation not found");
        return c;
    }
}
