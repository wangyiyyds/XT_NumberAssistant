export interface ApiErrorBody {
  code?: number
  message?: string
}

/** 后端统一响应外壳 Result<T> */
export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
  timestamp?: string
}

/** 后端统一分页壳 PageResult<T>（backend-manage common/PageResult.java） */
export interface PageResult<T> {
  total: number
  pages: number
  current: number
  size: number
  records: T[]
}
