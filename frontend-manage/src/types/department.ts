/** 部门（对应后端 DepartmentDto） */
export interface Department {
  id: string
  name: string
  enabled?: boolean
  brandOnly?: boolean
  sortOrder?: number
}

/** 部门新建/编辑请求体 */
export interface DepartmentSaveRequest {
  name: string
  enabled?: boolean
  brandOnly?: boolean
  sortOrder?: number
}
