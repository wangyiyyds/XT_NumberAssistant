import type { Menu, MenuSaveRequest } from '../types/menu'
import { request } from '../utils/request'

export async function fetchMenus(): Promise<Menu[]> {
  return request<Menu[]>('GET', '/api/menus')
}

export async function createMenu(body: MenuSaveRequest): Promise<Menu> {
  return request<Menu>('POST', '/api/menus', body)
}

export async function updateMenu(publicId: string, body: MenuSaveRequest): Promise<Menu> {
  return request<Menu>('PUT', `/api/menus/${publicId}`, body)
}

export async function deleteMenu(publicId: string): Promise<void> {
  await request<void>('DELETE', `/api/menus/${publicId}`)
}
