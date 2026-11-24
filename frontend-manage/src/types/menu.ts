/** 菜单（对应后端 MenuDto） */
export interface Menu {
  id: string
  name: string
  /** 前端路由路径，如 /agents */
  path: string
  /** @element-plus/icons-vue 导出名，如 Cpu；未知名回退默认图标 */
  icon?: string
  sortOrder: number
}

/** 菜单新建/编辑请求体 */
export interface MenuSaveRequest {
  name: string
  path: string
  icon?: string
  sortOrder: number
}
