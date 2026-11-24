import { defineStore } from 'pinia'

import { fetchMe, login as apiLogin, logout as apiLogout } from '../api/auth'
import { clearToken, getToken, setToken } from '../utils/storage'
import type { Admin } from '../types/admin'

interface AuthState {
  admin: Admin | null
  loaded: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({ admin: null, loaded: false }),
  getters: {
    isLoggedIn: () => !!getToken(),
  },
  actions: {
    async login(username: string, password: string) {
      const result = await apiLogin(username, password)
      setToken(result.token)
      this.admin = result.admin
      this.loaded = true
    },
    async load() {
      if (!getToken()) return
      try {
        this.admin = await fetchMe()
      } catch {
        // 401 由 request 统一跳转
      }
      this.loaded = true
    },
    logout() {
      apiLogout().catch(() => {})
      clearToken()
      this.admin = null
      this.loaded = false
    },
  },
})
