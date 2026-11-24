package com.xianting.manage.service;

import com.xianting.manage.dto.PromptDetailDto;
import com.xianting.manage.dto.PromptDto;
import com.xianting.manage.dto.PromptSaveRequest;

import java.util.List;

public interface PromptService {

    /** 某智能体的全部提示词版本（version 降序，不含正文） */
    List<PromptDto> listByAgent(String agentPublicId);

    /** 单个版本详情（含正文） */
    PromptDetailDto detail(String publicId);

    /**
     * 新建草稿版本：version = 当前最大版本 + 1，血缘默认挂当前 ACTIVE 版本，
     * variables/modelConfig 从血缘版本复制。
     */
    PromptDetailDto createDraft(String agentPublicId, PromptSaveRequest request, String adminUsername);

    /** 编辑草稿：仅 DRAFT 可改，ACTIVE/ARCHIVED 拒绝（409） */
    PromptDetailDto updateDraft(String publicId, PromptSaveRequest request, String adminUsername);

    /**
     * 发布草稿：事务内先归档该智能体当前 ACTIVE 版本，再把草稿置为 ACTIVE。
     * 并发双发布由 uk_agent_active 唯一索引兜底（DuplicateKeyException -> 409）。
     * 客户端 AiServiceImpl 只读 ACTIVE 版本，发布后立即生效，客户端零改动。
     */
    PromptDetailDto publish(String publicId, String adminUsername);

    /**
     * 回滚到历史版本：以目标 ARCHIVED 版本的内容生成一个新的 ACTIVE 版本
     * （version = max + 1，parent 指向目标版本），历史版本本身不可变。
     */
    PromptDetailDto rollback(String publicId, String adminUsername);
}
