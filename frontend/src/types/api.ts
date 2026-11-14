export interface ApiErrorBody {
  code?: number
  message?: string
}

export interface PageResult<T> {
  object: string
  data: T[]
  first_id: string | null
  last_id: string | null
  has_more: boolean
  after: string | null
}
