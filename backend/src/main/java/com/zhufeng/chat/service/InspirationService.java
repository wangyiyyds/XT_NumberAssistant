package com.zhufeng.chat.service;

import com.zhufeng.chat.dto.InspirationKeywordDto;

import java.util.Map;

/** 灵感模块业务接口 */
public interface InspirationService {

    /** 关键词 + 视频列表，返回 {data: {keywords: [...]}} */
    Map<String, Object> list(Long userId);

    /** 新增关键词 */
    InspirationKeywordDto addKeyword(Long userId, String keyword);

    /** 删除关键词及其视频 */
    Map<String, Object> deleteKeyword(Long userId, Long id);

    /** 付费刷新关键词缓存 */
    InspirationKeywordDto refresh(Long userId, Long id);
}
