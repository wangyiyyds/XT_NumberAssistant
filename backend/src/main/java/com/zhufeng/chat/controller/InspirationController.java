package com.zhufeng.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhufeng.chat.common.ApiException;
import com.zhufeng.chat.common.UserContext;
import com.zhufeng.chat.entity.Balance;
import com.zhufeng.chat.entity.InspirationKeyword;
import com.zhufeng.chat.entity.InspirationVideo;
import com.zhufeng.chat.mapper.BalanceMapper;
import com.zhufeng.chat.mapper.InspirationKeywordMapper;
import com.zhufeng.chat.mapper.InspirationVideoMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inspiration")
public class InspirationController {

    private static final int MAX_KEYWORDS = 5;
    private static final long REFRESH_COST = 60;

    private final InspirationKeywordMapper keywordMapper;
    private final InspirationVideoMapper videoMapper;
    private final BalanceMapper balanceMapper;

    public InspirationController(InspirationKeywordMapper keywordMapper, InspirationVideoMapper videoMapper,
                                 BalanceMapper balanceMapper) {
        this.keywordMapper = keywordMapper;
        this.videoMapper = videoMapper;
        this.balanceMapper = balanceMapper;
    }

    @GetMapping
    public Map<String, Object> inspiration() {
        Long userId = UserContext.require();
        QueryWrapper<InspirationKeyword> kwQw = new QueryWrapper<>();
        kwQw.eq("user_id", userId).orderByAsc("sort_order");
        List<Map<String, Object>> keywords = new ArrayList<>();
        for (InspirationKeyword kw : keywordMapper.selectList(kwQw)) {
            keywords.add(toVo(kw, videosOf(kw.getId())));
        }
        return Map.of("data", Map.of("keywords", keywords));
    }

    @PostMapping("/keywords")
    public Map<String, Object> addKeyword(@RequestBody Map<String, String> body) {
        Long userId = UserContext.require();
        String keyword = body.get("keyword");
        if (keyword == null || keyword.isBlank()) throw new ApiException(400, "keyword is required");
        keyword = keyword.trim();

        QueryWrapper<InspirationKeyword> countQw = new QueryWrapper<>();
        countQw.eq("user_id", userId);
        long count = keywordMapper.selectCount(countQw);
        if (count >= MAX_KEYWORDS) throw new ApiException(400, "最多" + MAX_KEYWORDS + "个关键词");

        QueryWrapper<InspirationKeyword> dupQw = new QueryWrapper<>();
        dupQw.eq("user_id", userId).eq("keyword", keyword);
        if (keywordMapper.selectCount(dupQw) > 0) throw new ApiException(409, "关键词已存在");

        InspirationKeyword kw = new InspirationKeyword();
        kw.setUserId(userId);
        kw.setKeyword(keyword);
        kw.setSortOrder((int) count);
        kw.setCacheExpiresAt(LocalDateTime.now().plusHours(24));
        kw.setCreatedAt(LocalDateTime.now());
        keywordMapper.insert(kw);
        return toVo(kw, List.of());
    }

    @DeleteMapping("/keywords/{id}")
    public Map<String, Object> deleteKeyword(@PathVariable Long id) {
        InspirationKeyword kw = requireOwn(id);
        QueryWrapper<InspirationVideo> vqw = new QueryWrapper<>();
        vqw.eq("keyword_id", kw.getId());
        videoMapper.delete(vqw);
        keywordMapper.deleteById(kw.getId());
        return Map.of("success", true);
    }

    /** Manual refresh: costs credits, bumps the cache window (videos come from the seeded pool). */
    @PostMapping("/keywords/{id}/refresh")
    public Map<String, Object> refresh(@PathVariable Long id) {
        Long userId = UserContext.require();
        InspirationKeyword kw = requireOwn(id);

        QueryWrapper<Balance> bqw = new QueryWrapper<>();
        bqw.eq("user_id", userId);
        Balance balance = balanceMapper.selectOne(bqw);
        long credits = balance == null || balance.getTokenCredits() == null ? 0 : balance.getTokenCredits();
        if (credits < REFRESH_COST) throw new ApiException(402, "积分不足，刷新需要" + REFRESH_COST + "积分");
        balance.setTokenCredits(credits - REFRESH_COST);
        balanceMapper.updateById(balance);

        kw.setCacheExpiresAt(LocalDateTime.now().plusHours(24));
        keywordMapper.updateById(kw);
        return toVo(kw, videosOf(kw.getId()));
    }

    private InspirationKeyword requireOwn(Long id) {
        Long userId = UserContext.require();
        QueryWrapper<InspirationKeyword> qw = new QueryWrapper<>();
        qw.eq("id", id).eq("user_id", userId);
        InspirationKeyword kw = keywordMapper.selectOne(qw);
        if (kw == null) throw new ApiException(404, "keyword not found");
        return kw;
    }

    private List<Map<String, Object>> videosOf(Long keywordId) {
        QueryWrapper<InspirationVideo> qw = new QueryWrapper<>();
        qw.eq("keyword_id", keywordId).orderByDesc("id");
        List<Map<String, Object>> out = new ArrayList<>();
        for (InspirationVideo v : videoMapper.selectList(qw)) {
            out.add(toVo(v));
        }
        return out;
    }

    private static Map<String, Object> toVo(InspirationKeyword kw, List<Map<String, Object>> videos) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_id", kw.getId());
        m.put("keyword", kw.getKeyword());
        m.put("order", kw.getSortOrder());
        m.put("cacheExpiresAt", kw.getCacheExpiresAt());
        m.put("videos", videos);
        return m;
    }

    private static Map<String, Object> toVo(InspirationVideo v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_id", v.getId());
        m.put("author", v.getAuthor());
        m.put("fans", v.getFans());
        m.put("duration", v.getDuration());
        m.put("publishDate", v.getPublishDate());
        m.put("title", v.getTitle());
        m.put("likes", v.getLikes());
        m.put("coverUrl", v.getCoverUrl());
        m.put("videoUrl", v.getVideoUrl());
        m.put("lowFansHighLikes", Boolean.TRUE.equals(v.getLowFansHighLikes()));
        return m;
    }
}
