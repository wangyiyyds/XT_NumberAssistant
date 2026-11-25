import type { Menu } from '../types/menu'
import { request } from '../utils/request'

/** 侧栏导航数据：菜单项只读，内容以后端种子数据为准，不提供增删改 */
export async function fetchMenus(): Promise<Menu[]> {
  return request<Menu[]>('GET', '/api/menus')
}
