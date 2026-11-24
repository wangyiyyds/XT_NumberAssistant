import type { PageResult } from '../types/api'
import type { UserAdmin } from '../types/user'
import { request } from '../utils/request'

export interface UserQuery {
  page: number
  size: number
  keyword?: string
  planId?: string
}

/** 新增用户请求体：planId 为套餐 publicId，留空表示暂不分配 */
export interface CreateUserPayload {
  email: string
  password: string
  name?: string
  planId?: string
}

export async function createUser(payload: CreateUserPayload): Promise<UserAdmin> {
  return request<UserAdmin>('POST', '/api/users', payload)
}

export async function fetchUsers(query: UserQuery): Promise<PageResult<UserAdmin>> {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('size', String(query.size))
  if (query.keyword) params.set('keyword', query.keyword)
  if (query.planId) params.set('planId', query.planId)
  return request<PageResult<UserAdmin>>('GET', `/api/users?${params.toString()}`)
}

/** planId 传 null 表示清除用户套餐 */
export async function assignPlan(userPublicId: string, planId: string | null): Promise<UserAdmin> {
  return request<UserAdmin>('PUT', `/api/users/${userPublicId}/plan`, { planId })
}

export async function setUserDisabled(userPublicId: string, disabled: boolean): Promise<UserAdmin> {
  return request<UserAdmin>('PUT', `/api/users/${userPublicId}/disabled`, { disabled })
}
