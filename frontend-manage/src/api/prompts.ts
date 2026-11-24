import type { Prompt, PromptDetail, PromptSaveRequest } from '../types/prompt'
import { request } from '../utils/request'

/** 某智能体的全部版本（version 降序，不含正文） */
export async function fetchPrompts(agentPublicId: string): Promise<Prompt[]> {
  return request<Prompt[]>('GET', `/api/agents/${agentPublicId}/prompts`)
}

/** 单个版本详情（含正文） */
export async function fetchPrompt(publicId: string): Promise<PromptDetail> {
  return request<PromptDetail>('GET', `/api/prompts/${publicId}`)
}

/** 基于当前 ACTIVE 版本新建草稿（version 自动 +1） */
export async function createDraft(agentPublicId: string, body: PromptSaveRequest): Promise<PromptDetail> {
  return request<PromptDetail>('POST', `/api/agents/${agentPublicId}/prompts`, body)
}

/** 编辑草稿（仅 DRAFT 可改） */
export async function updateDraft(publicId: string, body: PromptSaveRequest): Promise<PromptDetail> {
  return request<PromptDetail>('PUT', `/api/prompts/${publicId}`, body)
}

/** 发布：旧 ACTIVE 自动归档，本版本置 ACTIVE，客户端立即生效 */
export async function publishPrompt(publicId: string): Promise<PromptDetail> {
  return request<PromptDetail>('POST', `/api/prompts/${publicId}/publish`)
}

/** 回滚：以该历史版本内容生成新的 ACTIVE 版本 */
export async function rollbackPrompt(publicId: string): Promise<PromptDetail> {
  return request<PromptDetail>('POST', `/api/prompts/${publicId}/rollback`)
}
