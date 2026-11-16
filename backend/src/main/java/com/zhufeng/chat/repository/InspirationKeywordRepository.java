package com.zhufeng.chat.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.zhufeng.chat.entity.InspirationKeyword;

import java.util.List;

/** 灵感关键词数据仓储接口，继承 MyBatis-Plus IRepository */
public interface InspirationKeywordRepository extends IRepository<InspirationKeyword> {

    /** 用户关键词列表，按 sort_order 升序 */
    List<InspirationKeyword> listByUserId(Long userId);

    long countByUserId(Long userId);

    boolean existsByUserIdAndKeyword(Long userId, String keyword);

    InspirationKeyword findByIdAndUserId(Long id, Long userId);


}
