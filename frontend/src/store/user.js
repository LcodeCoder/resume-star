/**
 * 用户状态 Store
 * 功能：维护登录用户、会员等级、AI 次数、导出次数等全局展示数据
 */
import { defineStore } from 'pinia'
import { getMe, login, logout, register } from '../api/user'

/** 用户资料本地缓存键 */
const USER_PROFILE_KEY = 'resume_lcode_user_profile'

const readCachedProfile = () => {
  try {
    const raw = localStorage.getItem(USER_PROFILE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    localStorage.removeItem(USER_PROFILE_KEY)
    return null
  }
}

const cacheProfile = (profile) => {
  if (profile) localStorage.setItem(USER_PROFILE_KEY, JSON.stringify(profile))
  else localStorage.removeItem(USER_PROFILE_KEY)
}

/** 兼容后端返回 profile 或 { token, profile } 两种结构 */
const normalizeProfile = (data) => data?.profile || data

let profileLoadingPromise = null

export const useUserStore = defineStore('user', {
  state: () => ({
    profile: readCachedProfile(),
    authReady: false
  }),
  getters: {
    isLoggedIn: (state) => !!state.profile,
    vipLevel: (state) => state.profile?.vipLevel || 'FREE',
    vipLevelLabel: (state) => {
      const labelMap = { FREE: '免费版', BASIC: '基础会员', PRO: '高级会员', SVIP: '超级会员' }
      const level = state.profile?.vipLevel || 'FREE'
      return labelMap[level] || level
    },
    remainingAiQuota: (state) => state.profile?.remainingAiQuota || 0,
    remainingExportQuota: (state) => state.profile?.remainingExportQuota || 0
  },
  actions: {
    /** 账号密码登录 */
    async login(payload) {
      this.profile = normalizeProfile(await login(payload))
      this.authReady = true
      cacheProfile(this.profile)
      return this.profile
    },
    /** 用户注册（成功后自动登录） */
    async register(payload) {
      this.profile = normalizeProfile(await register(payload))
      this.authReady = true
      cacheProfile(this.profile)
      return this.profile
    },
    /** 登出 */
    async logout() {
      try {
        await logout()
      } finally {
        this.profile = null
        this.authReady = true
        cacheProfile(null)
      }
    },
    /** 从 Session 恢复当前用户：未登录时静默返回 null，不抛错 */
    async loadProfile() {
      if (profileLoadingPromise) return profileLoadingPromise
      profileLoadingPromise = (async () => {
        try {
          this.profile = normalizeProfile(await getMe())
          cacheProfile(this.profile)
        } catch (e) {
          this.profile = null
          cacheProfile(null)
        } finally {
          this.authReady = true
          profileLoadingPromise = null
        }
        return this.profile
      })()
      return profileLoadingPromise
    }
  }
})

