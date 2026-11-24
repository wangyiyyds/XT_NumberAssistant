import type { Department } from './department'

/** 套餐（对应后端 PlanDto），departments 为该套餐开放的部门 */
export interface Plan {
  id: string
  name: string
  level: number
  description?: string
  departments?: Department[]
  createdAt?: string
  updatedAt?: string
}

/** 套餐新建/编辑请求体 */
export interface PlanSaveRequest {
  name: string
  level: number
  description?: string
}
