import type { ApiErrorBody } from '../types/api'
import { clearToken, getToken } from './storage'

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export async function request<T>(
  method: string,
  path: string,
  body?: unknown,
): Promise<T> {
  const token = getToken()
  const headers: Record<string, string> = {
    Accept: 'application/json',
  }
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const response = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (response.status === 401) {
    clearToken()
    if (!window.location.pathname.startsWith('/login')) {
      window.location.href = '/login'
    }
    throw new ApiError('未登录或登录已过期', 401)
  }

  if (!response.ok) {
    let message = `请求失败 (${response.status})`
    try {
      const data = (await response.json()) as ApiErrorBody
      if (data.message) message = data.message
    } catch {
      // 非 JSON 错误体，使用默认文案
    }
    throw new ApiError(message, response.status)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}
