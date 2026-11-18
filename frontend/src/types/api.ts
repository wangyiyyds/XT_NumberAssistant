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

export interface PageResult<T> {
  object: string
  data: T[]
  first_id: string | null
  last_id: string | null
  has_more: boolean
  after: string | null
}
