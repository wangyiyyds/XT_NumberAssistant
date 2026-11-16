package com.zhufeng.chat.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.zhufeng.chat.entity.InspirationVideo;

import java.util.List;

/** 灵感视频数据仓储接口，继承 MyBatis-Plus IRepository */
public interface InspirationVideoRepository extends IRepository<InspirationVideo> {

    /** 关键词下视频，按 id 倒序 */
    List<InspirationVideo> listByKeywordId(Long keywordId);


    void deleteByKeywordId(Long keywordId);
}
