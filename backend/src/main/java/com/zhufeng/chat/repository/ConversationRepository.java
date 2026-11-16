package com.zhufeng.chat.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.zhufeng.chat.entity.Conversation;

import java.util.List;

/** 会话数据仓储接口，继承 MyBatis-Plus IRepository */
public interface ConversationRepository extends IRepository<Conversation> {

    /** 用户会话列表，按 updated_at 倒序；agentId 为 null 表示不过滤 */
    List<Conversation> listByUser(Long userId, Long agentId);

    Conversation findByPublicIdAndUserId(String publicId, Long userId);


}
