package com.zhufeng.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.zhufeng.chat.entity.Conversation;
import com.zhufeng.chat.mapper.ConversationMapper;
import com.zhufeng.chat.repository.ConversationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ConversationRepositoryImpl extends CrudRepository<ConversationMapper, Conversation> implements ConversationRepository {

    @Override
    public List<Conversation> listByUser(Long userId, Long agentId) {
        QueryWrapper<Conversation> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        if (agentId != null) {
            qw.eq("agent_id", agentId);
        }
        qw.orderByDesc("updated_at");
        return getBaseMapper().selectList(qw);
    }

    @Override
    public Conversation findByPublicIdAndUserId(String publicId, Long userId) {
        QueryWrapper<Conversation> qw = new QueryWrapper<>();
        qw.eq("public_id", publicId).eq("user_id", userId);
        return getBaseMapper().selectOne(qw);
    }

}
