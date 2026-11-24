import type { Admin, AdminSaveRequest } from '../types/admin'
import { request } from '../utils/request'

export async function fetchAdmins(): Promise<Admin[]> {
  return request<Admin[]>('GET', '/api/admins')
}

export async function createAdmin(body: AdminSaveRequest): Promise<Admin> {
  return request<Admin>('POST', '/api/admins', body)
}
