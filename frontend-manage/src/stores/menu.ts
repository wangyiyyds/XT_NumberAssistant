import { defineStore } from 'pinia'

import { fetchMenus } from '../api/menu'
import type { Menu } from '../types/menu'

interface MenuState {
  menus: Menu[]
  loaded: boolean
}

export const useMenuStore = defineStore('menu', {
  state: (): MenuState => ({ menus: [], loaded: false }),
  actions: {
    async load() {
      if (this.loaded) return
      try {
        this.menus = await fetchMenus()
      } catch {
        // 401 由 request 统一跳转；其余错误侧栏保持空即可
      }
      this.loaded = true
    },
    /** 菜单管理页增删改后强制刷新，侧栏即时同步 */
    async reload() {
      this.menus = await fetchMenus()
      this.loaded = true
    },
  },
})
