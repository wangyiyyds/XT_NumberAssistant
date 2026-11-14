import { defineStore } from 'pinia'

import {
  fetchBalance,
  fetchMe,
  login as apiLogin,
  logout as apiLogout,
  register as apiRegister,
} from '../api/auth'
import { clearToken, getToken, setToken } from '../utils/storage'
import type { Balance, User } from '../types/user'

interface AuthState {
  user: User | null
  balance: Balance | null
  loaded: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({ user: null, balance: null, loaded: false }),
  getters: {
    isLoggedIn: () => !!getToken(),
  },
  actions: {
    async login(email: string, password: string) {
      const result = await apiLogin(email, password)
      setToken(result.token)
      this.user = result.user
      await this.loadBalance()
      this.loaded = true
    },
    async register(email: string, password: string, name: string) {
      const result = await apiRegister(email, password, name)
      setToken(result.token)
      this.user = result.user
      await this.loadBalance()
      this.loaded = true
    },
    async load() {
      if (!getToken()) return
      try {
        this.user = await fetchMe()
        await this.loadBalance()
      } catch {
        // 401 由 request 统一跳转
      }
      this.loaded = true
    },
    async loadBalance() {
      try {
        this.balance = await fetchBalance()
      } catch {
        // 余额加载失败不阻塞页面
      }
    },
    logout() {
      apiLogout().catch(() => {})
      clearToken()
      this.user = null
      this.balance = null
      this.loaded = false
    },
  },
})
