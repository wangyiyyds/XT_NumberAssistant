import { defineStore } from 'pinia'
import { api, getToken, setToken } from '../api/client'
import type { BalanceVo, UserVo } from '../types'

interface AuthState {
  user: UserVo | null
  balance: BalanceVo | null
  loaded: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({ user: null, balance: null, loaded: false }),
  getters: {
    isLoggedIn: () => !!getToken()
  },
  actions: {
    async login(email: string, password: string) {
      const res = await api.post<{ token: string; user: UserVo }>('/api/auth/login', {
        brand: 'zhufeng', email, password
      })
      setToken(res.token)
      this.user = res.user
      await this.loadBalance()
      this.loaded = true
    },
    async register(email: string, password: string, name: string) {
      const res = await api.post<{ token: string; user: UserVo }>('/api/auth/register', {
        email, password, name
      })
      setToken(res.token)
      this.user = res.user
      await this.loadBalance()
      this.loaded = true
    },
    async load() {
      if (!getToken()) return
      try {
        this.user = await api.get<UserVo>('/api/user')
        await this.loadBalance()
      } catch { /* 401 handled in client */ }
      this.loaded = true
    },
    async loadBalance() {
      try {
        this.balance = await api.get<BalanceVo>('/api/balance')
      } catch { /* ignore */ }
    },
    logout() {
      api.post('/api/auth/logout').catch(() => {})
      setToken(null)
      this.user = null
      this.balance = null
      this.loaded = false
    }
  }
})
