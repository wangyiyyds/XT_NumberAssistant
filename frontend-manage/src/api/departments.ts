import type { Department, DepartmentSaveRequest } from '../types/department'
import { request } from '../utils/request'

export async function fetchDepartments(): Promise<Department[]> {
  return request<Department[]>('GET', '/api/departments')
}

export async function createDepartment(body: DepartmentSaveRequest): Promise<Department> {
  return request<Department>('POST', '/api/departments', body)
}

export async function updateDepartment(publicId: string, body: DepartmentSaveRequest): Promise<Department> {
  return request<Department>('PUT', `/api/departments/${publicId}`, body)
}

export async function deleteDepartment(publicId: string): Promise<void> {
  await request<void>('DELETE', `/api/departments/${publicId}`)
}
