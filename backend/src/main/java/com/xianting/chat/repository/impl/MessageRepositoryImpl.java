package com.xianting.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.xianting.chat.entity.Message;
import com.xianting.chat.mapper.MessageMapper;
import com.xianting.chat.repository.MessageRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MessageRepositoryImpl extends CrudRepository<MessageMapper, Message> implements MessageRepository {

    @Override
    public List<Message> listByConversationId(Long conversationId) {
        QueryWrapper<Message> qw = new QueryWrapper<>();
        qw.eq("conversation_id", conversationId).orderByAsc("id");
        return getBaseMapper().selectList(qw);
    }

    @Override
    public void deleteByConversationId(Long conversationId) {
        QueryWrapper<Message> qw = new QueryWrapper<>();
        qw.eq("conversation_id", conversationId);
        getBaseMapper().delete(qw);
    }
}
