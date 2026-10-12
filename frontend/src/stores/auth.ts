import { defineStore } from 'pinia'
import http from '../utils/request'
import type { LoginResponse, UserVO } from '../types'

const ACCESS_KEY = 'crjy_access_token'
const REFRESH_KEY = 'crjy_refresh_token'
const USER_KEY = 'crjy_user'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem(ACCESS_KEY) || '',
    refreshToken: localStorage.getItem(REFRESH_KEY) || '',
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null') as UserVO | null
  }),
  getters: {
    isLoggedIn: (state) => !!state.accessToken,
    isAdmin: (state) => !!state.user && state.user.roles.includes('ADMIN')
  },
  actions: {
    saveTokens(response: LoginResponse) {
      this.accessToken = response.accessToken
      this.refreshToken = response.refreshToken
      this.user = response.user
      localStorage.setItem(ACCESS_KEY, response.accessToken)
      localStorage.setItem(REFRESH_KEY, response.refreshToken)
      localStorage.setItem(USER_KEY, JSON.stringify(response.user))
    },

    async login(payload: { username: string; password: string }) {
      const result: any = await http.post('/auth/login', payload)
      this.saveTokens(result.data as LoginResponse)
    },

    async register(payload: { username: string; password: string; email?: string }) {
      const result: any = await http.post('/auth/register', payload)
      this.saveTokens(result.data as LoginResponse)
    },

    async tryRefresh(): Promise<boolean> {
      if (!this.refreshToken) return false
      try {
        const result: any = await http.post('/auth/refresh', { refreshToken: this.refreshToken })
        this.saveTokens(result.data as LoginResponse)
        return true
      } catch {
        return false
      }
    },

    async fetchMe() {
      if (this.user) return
      try {
        const result: any = await http.get('/user/me')
        this.user = result.data as UserVO
        localStorage.setItem(USER_KEY, JSON.stringify(this.user))
      } catch {
        // 由拦截器统一提示
      }
    },

    logout() {
      http.post('/auth/logout').catch(() => {
        // 令牌将被前端丢弃，后端审计失败不阻塞登出
      })
      this.accessToken = ''
      this.refreshToken = ''
      this.user = null
      localStorage.removeItem(ACCESS_KEY)
      localStorage.removeItem(REFRESH_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
