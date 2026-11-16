package com.zhufeng.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.zhufeng.chat.entity.InspirationKeyword;
import com.zhufeng.chat.mapper.InspirationKeywordMapper;
import com.zhufeng.chat.repository.InspirationKeywordRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InspirationKeywordRepositoryImpl extends CrudRepository<InspirationKeywordMapper, InspirationKeyword>
        implements InspirationKeywordRepository {

    @Override
    public List<InspirationKeyword> listByUserId(Long userId) {
        QueryWrapper<InspirationKeyword> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).orderByAsc("sort_order");
        return getBaseMapper().selectList(qw);
    }

    @Override
    public long countByUserId(Long userId) {
        QueryWrapper<InspirationKeyword> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        Long count = getBaseMapper().selectCount(qw);
        return count == null ? 0 : count;
    }

    @Override
    public boolean existsByUserIdAndKeyword(Long userId, String keyword) {
        QueryWrapper<InspirationKeyword> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("keyword", keyword);
        Long count = getBaseMapper().selectCount(qw);
        return count != null && count > 0;
    }

    @Override
    public InspirationKeyword findByIdAndUserId(Long id, Long userId) {
        QueryWrapper<InspirationKeyword> qw = new QueryWrapper<>();
        qw.eq("id", id).eq("user_id", userId);
        return getBaseMapper().selectOne(qw);
    }

}
