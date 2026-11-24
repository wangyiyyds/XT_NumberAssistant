import type { Plan, PlanSaveRequest } from '../types/plan'
import { request } from '../utils/request'

export async function fetchPlans(): Promise<Plan[]> {
  return request<Plan[]>('GET', '/api/plans')
}

export async function fetchPlan(publicId: string): Promise<Plan> {
  return request<Plan>('GET', `/api/plans/${publicId}`)
}

export async function createPlan(body: PlanSaveRequest): Promise<Plan> {
  return request<Plan>('POST', '/api/plans', body)
}

export async function updatePlan(publicId: string, body: PlanSaveRequest): Promise<Plan> {
  return request<Plan>('PUT', `/api/plans/${publicId}`, body)
}

/** 全量替换该套餐开放的部门（departmentIds 为部门 publicId 列表） */
export async function setPlanDepartments(publicId: string, departmentIds: string[]): Promise<Plan> {
  return request<Plan>('PUT', `/api/plans/${publicId}/departments`, { departmentIds })
}

export async function deletePlan(publicId: string): Promise<void> {
  await request<void>('DELETE', `/api/plans/${publicId}`)
}
