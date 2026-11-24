import type { AgentAdmin, AgentDetail, AgentSaveRequest } from '../types/agent'
import type { PageResult } from '../types/api'
import { request } from '../utils/request'

export interface AgentQuery {
  page: number
  size: number
  departmentId?: string
  keyword?: string
}

export async function fetchAgents(query: AgentQuery): Promise<PageResult<AgentAdmin>> {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('size', String(query.size))
  if (query.departmentId) params.set('departmentId', query.departmentId)
  if (query.keyword) params.set('keyword', query.keyword)
  return request<PageResult<AgentAdmin>>('GET', `/api/agents?${params.toString()}`)
}

export async function fetchAgent(publicId: string): Promise<AgentDetail> {
  return request<AgentDetail>('GET', `/api/agents/${publicId}`)
}

/** 创建智能体：后端事务内自动落 v1 ACTIVE 提示词 */
export async function createAgent(body: AgentSaveRequest): Promise<AgentDetail> {
  return request<AgentDetail>('POST', '/api/agents', body)
}

export async function updateAgent(publicId: string, body: AgentSaveRequest): Promise<AgentDetail> {
  return request<AgentDetail>('PUT', `/api/agents/${publicId}`, body)
}

/** 删除智能体：其全部提示词版本由数据库外键级联删除 */
export async function deleteAgent(publicId: string): Promise<void> {
  await request<void>('DELETE', `/api/agents/${publicId}`)
}
