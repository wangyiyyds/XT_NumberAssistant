import type { Admin, LoginResult } from '../types/admin'
import { request } from '../utils/request'

export async function login(username: string, password: string): Promise<LoginResult> {
  return request<LoginResult>('POST', '/api/auth/login', { username, password })
}

export async function fetchMe(): Promise<Admin> {
  return request<Admin>('GET', '/api/auth/me')
}

export async function logout(): Promise<void> {
  await request<void>('POST', '/api/auth/logout')
}
