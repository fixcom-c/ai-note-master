import { defineStore } from 'pinia'
import { authAPI } from '@/api'
import type { LoginRequest } from '@/api/types'

interface UserState {
  token: string | null
  userId: number | null
  username: string | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: localStorage.getItem('token'),
    userId: localStorage.getItem('userId') ? Number(localStorage.getItem('userId')) : null,
    username: localStorage.getItem('username')
  }),

  actions: {
    async login(loginForm: LoginRequest) {
      const res = await authAPI.login(loginForm)
      this.token = res.token
      this.userId = res.userId
      this.username = res.username
      localStorage.setItem('token', res.token)
      localStorage.setItem('userId', String(res.userId))
      localStorage.setItem('username', res.username)
      return res
    },

    logout() {
      this.token = null
      this.userId = null
      this.username = null
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
    }
  }
})
