import axios from 'axios'
import type { AxiosError } from 'axios'

import type { ApiErrorBody } from '../types/api'
import { brandData, brandText } from './brand'
import { clearToken, getToken } from './storage'

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export const http = axios.create({
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorBody>) => {
    const status = error.response?.status ?? 0

    if (status === 401) {
      clearToken()
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
      return Promise.reject(new ApiError('未登录或登录已过期', 401))
    }

    const body = error.response?.data
    const rawMessage = body?.message || error.message || '请求失败'
    return Promise.reject(new ApiError(brandText(rawMessage), status))
  },
)

export async function request<T>(
  method: string,
  path: string,
  body?: unknown,
): Promise<T> {
  const response = await http.request<T>({
    method,
    url: path,
    data: body,
  })
  // 显示层品牌兜底：接口数据里的旧品牌名统一换成新品牌名
  return brandData(response.data)
}
