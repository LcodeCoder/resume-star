/**
 * 用户状态 Store
 * 功能：维护登录用户、会员等级、AI 次数、导出次数等全局展示数据
 */
import { defineStore } from 'pinia'
import { getMe, getUserQuota, login, logout, register } from '../api/user'

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
    authReady: false,
    quota: null
  }),
  getters: {
    isLoggedIn: (state) => !!state.profile,
    vipLevel: (state) => state.profile?.vipLevel || null,
    vipLevelLabel: (state) => state.profile?.vipLevel || '免费版',
    /** 今日剩余 AI 次数：优先用真实额度（不限制时显示「不限」），回退到资料字段 */
    remainingAiQuota: (state) => {
      if (state.quota) return state.quota.aiUnlimited ? '不限' : (state.quota.aiRemaining ?? 0)
      return state.profile?.remainingAiQuota ?? 0
    },
    /** 今日剩余导出次数 */
    remainingExportQuota: (state) => {
      if (state.quota) return state.quota.exportUnlimited ? '不限' : (state.quota.exportRemaining ?? 0)
      return state.profile?.remainingExportQuota ?? 0
    },
    /** 额度兑换码累计 AI 次数余额（充值卡，跨日保留） */
    aiBalance: (state) => state.quota?.aiBalance ?? 0,
    /** 额度兑换码累计导出次数余额（充值卡，跨日保留） */
    exportBalance: (state) => state.quota?.exportBalance ?? 0,
    /** 额度兑换码累计模拟面试次数余额（充值卡，跨日保留） */
    interviewBalance: (state) => state.quota?.interviewBalance ?? 0,
    /**
     * 可用 AI 次数总和（导航栏汇总展示）：
     *   不限制 → 「不限」；否则 = 今日剩余 + 充值卡余额
     */
    totalAiQuota(state) {
      if (state.quota?.aiUnlimited) return '不限'
      const daily = state.quota ? (state.quota.aiRemaining ?? 0) : (state.profile?.remainingAiQuota ?? 0)
      const balance = state.quota?.aiBalance ?? 0
      return Number(daily) + Number(balance)
    },
    /** 可用导出次数总和（同上） */
    totalExportQuota(state) {
      if (state.quota?.exportUnlimited) return '不限'
      const daily = state.quota ? (state.quota.exportRemaining ?? 0) : (state.profile?.remainingExportQuota ?? 0)
      const balance = state.quota?.exportBalance ?? 0
      return Number(daily) + Number(balance)
    }
  },
  actions: {
    /** 账号密码登录 */
    async login(payload) {
      this.profile = normalizeProfile(await login(payload))
      this.authReady = true
      cacheProfile(this.profile)
      this.loadQuota()
      return this.profile
    },
    /** 用户注册（成功后自动登录） */
    async register(payload) {
      this.profile = normalizeProfile(await register(payload))
      this.authReady = true
      cacheProfile(this.profile)
      this.loadQuota()
      return this.profile
    },
    /** 登出 */
    async logout() {
      try {
        await logout()
      } finally {
        this.profile = null
        this.quota = null
        this.authReady = true
        cacheProfile(null)
      }
    },
    /** 拉取当前用户今日真实额度（AI/导出 上限、已用、剩余） */
    async loadQuota() {
      try {
        this.quota = await getUserQuota()
      } catch (e) {
        this.quota = null
      }
      return this.quota
    },
    /** 从 Session 恢复当前用户：未登录时静默返回 null，不抛错 */
    async loadProfile() {
      if (profileLoadingPromise) return profileLoadingPromise
      profileLoadingPromise = (async () => {
        try {
          this.profile = normalizeProfile(await getMe())
          cacheProfile(this.profile)
          this.loadQuota()
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

