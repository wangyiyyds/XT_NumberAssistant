/** 提示词版本列表行（对应后端 PromptDto，不含正文） */
export interface Prompt {
  id: string
  agentId?: string
  version: number
  versionTag?: string
  name?: string
  description?: string
  promptType?: string
  /** DRAFT / ACTIVE / ARCHIVED */
  status: string
  /** 血缘：基于哪个版本号改出来的，首版为空 */
  parentVersion?: number
  createdBy?: string
  updatedBy?: string
  createdAt?: string
  updatedAt?: string
  publishedAt?: string
  archivedAt?: string
}

/** 提示词版本详情（对应后端 PromptDetailDto，含正文） */
export interface PromptDetail extends Prompt {
  content: string
  variables?: string
  modelConfig?: string
}

/** 草稿新建/编辑请求体 */
export interface PromptSaveRequest {
  name?: string
  description?: string
  versionTag?: string
  content: string
}
