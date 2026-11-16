package com.zhufeng.chat.service.impl;

import com.zhufeng.chat.common.ApiException;
import com.zhufeng.chat.converter.InspirationKeywordConverter;
import com.zhufeng.chat.dto.InspirationKeywordDto;
import com.zhufeng.chat.entity.InspirationKeyword;
import com.zhufeng.chat.repository.InspirationKeywordRepository;
import com.zhufeng.chat.repository.InspirationVideoRepository;
import com.zhufeng.chat.service.BalanceService;
import com.zhufeng.chat.service.InspirationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class InspirationServiceImpl implements InspirationService {

    private static final int MAX_KEYWORDS = 5;
    private static final long REFRESH_COST = 60;

    private final InspirationKeywordRepository keywordRepository;
    private final InspirationVideoRepository videoRepository;
    private final BalanceService balanceService;
    private final InspirationKeywordConverter keywordConverter;

    public InspirationServiceImpl(InspirationKeywordRepository keywordRepository,
                                  InspirationVideoRepository videoRepository,
                                  BalanceService balanceService,
                                  InspirationKeywordConverter keywordConverter) {
        this.keywordRepository = keywordRepository;
        this.videoRepository = videoRepository;
        this.balanceService = balanceService;
        this.keywordConverter = keywordConverter;
    }

    @Override
    public Map<String, Object> list(Long userId) {
        List<InspirationKeywordDto> keywords = new ArrayList<>();
        for (InspirationKeyword kw : keywordRepository.listByUserId(userId)) {
            keywords.add(keywordConverter.toDto(kw, videoRepository.listByKeywordId(kw.getId())));
        }
        return Map.of("data", Map.of("keywords", keywords));
    }

    @Override
    public InspirationKeywordDto addKeyword(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank()) throw new ApiException(400, "keyword is required");
        keyword = keyword.trim();

        long count = keywordRepository.countByUserId(userId);
        if (count >= MAX_KEYWORDS) throw new ApiException(400, "最多" + MAX_KEYWORDS + "个关键词");
        if (keywordRepository.existsByUserIdAndKeyword(userId, keyword)) {
            throw new ApiException(409, "关键词已存在");
        }

        InspirationKeyword kw = new InspirationKeyword();
        kw.setUserId(userId);
        kw.setKeyword(keyword);
        kw.setSortOrder((int) count);
        kw.setCacheExpiresAt(LocalDateTime.now().plusHours(24));
        kw.setCreatedAt(LocalDateTime.now());
        keywordRepository.save(kw);
        return keywordConverter.toDto(kw, List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteKeyword(Long userId, Long id) {
        InspirationKeyword kw = requireOwn(id, userId);
        videoRepository.deleteByKeywordId(kw.getId());
        keywordRepository.removeById(kw.getId());
        return Map.of("success", true);
    }

    /** 手动刷新：扣积分并续缓存窗口（视频来自种子数据池）。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspirationKeywordDto refresh(Long userId, Long id) {
        InspirationKeyword kw = requireOwn(id, userId);
        balanceService.deductOrThrow(userId, REFRESH_COST);
        kw.setCacheExpiresAt(LocalDateTime.now().plusHours(24));
        keywordRepository.updateById(kw);
        return keywordConverter.toDto(kw, videoRepository.listByKeywordId(kw.getId()));
    }

    private InspirationKeyword requireOwn(Long id, Long userId) {
        InspirationKeyword kw = keywordRepository.findByIdAndUserId(id, userId);
        if (kw == null) throw new ApiException(404, "keyword not found");
        return kw;
    }
}
