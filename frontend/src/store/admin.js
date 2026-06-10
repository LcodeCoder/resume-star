/**
 * 管理员登录态 Store
 * 功能：维护管理员登录状态，供后台路由守卫与登出使用
 */
import { defineStore } from 'pinia'
import { adminLogin, adminLogout, getAdminMe } from '../api/adminAuth'

export const useAdminStore = defineStore('admin', {
  state: () => ({
    profile: null
  }),
  getters: {
    isLoggedIn: (state) => !!state.profile
  },
  actions: {
    async login(payload) {
      this.profile = await adminLogin(payload)
      return this.profile
    },
    async logout() {
      try {
        await adminLogout()
      } finally {
        this.profile = null
      }
    },
    /** 从 Session 恢复管理员登录：未登录时静默返回 null */
    async loadProfile() {
      try {
        this.profile = await getAdminMe()
      } catch (e) {
        this.profile = null
      }
      return this.profile
    }
  }
})
