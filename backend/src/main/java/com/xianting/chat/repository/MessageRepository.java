package com.xianting.chat.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.xianting.chat.entity.Message;

import java.util.List;

/** 消息数据仓储接口，继承 MyBatis-Plus IRepository */
public interface MessageRepository extends IRepository<Message> {

    /** 会话内消息，按 id 升序 */
    List<Message> listByConversationId(Long conversationId);


    void deleteByConversationId(Long conversationId);
}
