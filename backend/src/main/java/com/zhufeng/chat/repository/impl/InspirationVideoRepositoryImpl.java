package com.zhufeng.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.zhufeng.chat.entity.InspirationVideo;
import com.zhufeng.chat.mapper.InspirationVideoMapper;
import com.zhufeng.chat.repository.InspirationVideoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InspirationVideoRepositoryImpl extends CrudRepository<InspirationVideoMapper, InspirationVideo>
        implements InspirationVideoRepository {

    @Override
    public List<InspirationVideo> listByKeywordId(Long keywordId) {
        QueryWrapper<InspirationVideo> qw = new QueryWrapper<>();
        qw.eq("keyword_id", keywordId).orderByDesc("id");
        return getBaseMapper().selectList(qw);
    }

    @Override
    public void deleteByKeywordId(Long keywordId) {
        QueryWrapper<InspirationVideo> qw = new QueryWrapper<>();
        qw.eq("keyword_id", keywordId);
        getBaseMapper().delete(qw);
    }
}
