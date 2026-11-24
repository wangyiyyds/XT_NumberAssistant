import type { PromptDetail } from './prompt'

/** 智能体列表行（对应后端 AgentAdminDto） */
export interface AgentAdmin {
  id: string
  departmentId?: string
  departmentName?: string
  name: string
  description?: string
  avatar?: string
  category?: string
  isPreset?: boolean
  conversationStarters?: string[]
  /** 当前生效提示词版本号，从未发布过为空 */
  activePromptVersion?: number
  createdAt?: string
}

/** 智能体详情（对应后端 AgentDetailDto） */
export interface AgentDetail extends AgentAdmin {
  instructions?: string
  activePrompt?: PromptDetail
}

/** 智能体新建/编辑请求体（promptContent 仅创建时使用） */
export interface AgentSaveRequest {
  departmentId: string
  name: string
  description?: string
  avatar?: string
  category?: string
  isPreset?: boolean
  conversationStarters?: string[]
  instructions?: string
  promptContent?: string
}
