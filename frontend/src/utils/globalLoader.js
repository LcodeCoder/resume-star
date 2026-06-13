/**
 * 全局加载状态
 * 功能：统一管理顶部进度条与 Lcode 拼字遮罩；axios 通过 start/doneLoading 接入，
 *       路由通过 beginNav/endNav 接入。
 * 设计：
 *  - 进度条：只要「导航中」或「有在途请求」就持续流动，结束后跳 100% 收尾再复位。
 *  - 遮罩：用「导航阶段(navPhase)」布尔驱动，而非计数 —— 重定向(next 跳转)再多次也不会泄漏。
 *          导航开始进入 navPhase，并把它延续到首屏 onMounted 数据请求结束，
 *          所以模板库 / 编辑器等「切页后才加载数据」的页面也能一直显示遮罩。
 *          瞬时完成的页面(无数据请求)则不会闪遮罩。
 */
import { reactive } from 'vue'

const state = reactive({
  count: 0, // 在途的非静默 axios 请求数（决定顶部进度条）
  overlay: false, // 是否展示 Lcode 全屏遮罩
  tick: 0 // 顶部进度条 0~100
})

// 是否仍在加载：路由切换中，或有在途请求
const isBusy = () => routing || state.count > 0

/* ----------------------------- 顶部进度条 ----------------------------- */
let progressTimer = null
let resetTimer = null

/** 推进进度条：靠近 95% 时变慢，保证视觉一直在动 */
const stepProgress = () => {
  const remain = 95 - state.tick
  state.tick = Math.min(95, state.tick + Math.max(0.4, remain / 18))
}

const runProgress = () => {
  if (resetTimer) {
    clearTimeout(resetTimer)
    resetTimer = null
  }
  if (progressTimer) return
  state.tick = Math.max(state.tick, 8)
  progressTimer = setInterval(stepProgress, 220)
}

const finishProgress = () => {
  // 从未启动过就别闪一下 100%
  if (!progressTimer && state.tick === 0) return
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
  state.tick = 100
  if (resetTimer) clearTimeout(resetTimer)
  resetTimer = setTimeout(() => {
    resetTimer = null
    if (!isBusy()) state.tick = 0
  }, 320)
}

const refreshProgress = () => {
  if (isBusy()) runProgress()
  else finishProgress()
}

/* ------------------------------ Lcode 遮罩 ----------------------------- */
let routing = false // beforeEach ~ afterEach 之间
let navPhase = false // 「路由切换 + 首屏加载」整段阶段
let showTimer = null
let settleTimer = null

/** 延迟展示：220ms 后若仍在加载才显示遮罩，避免瞬时切换闪一下 */
const armShow = () => {
  if (showTimer || state.overlay) return
  showTimer = setTimeout(() => {
    showTimer = null
    if (navPhase && isBusy()) state.overlay = true
  }, 220)
}

/** 结束导航阶段：隐藏遮罩并清理待显示定时器 */
const endNavPhase = () => {
  navPhase = false
  if (showTimer) {
    clearTimeout(showTimer)
    showTimer = null
  }
  state.overlay = false
}

/**
 * 收尾判定：导航已结束且没有在途请求后，留一小段缓冲等首屏 onMounted 发起请求；
 * 若期间确实没有新请求(纯静态页)则结束导航阶段、隐藏遮罩。
 */
const armSettle = () => {
  if (settleTimer) clearTimeout(settleTimer)
  settleTimer = setTimeout(() => {
    settleTimer = null
    if (navPhase && !isBusy()) endNavPhase()
  }, 200)
}

/* ------------------------------ 对外接口 ------------------------------ */

/** axios 请求开始（顶部进度条；若处于导航阶段则一并维持遮罩） */
export const startLoading = () => {
  state.count += 1
  refreshProgress()
  if (navPhase) armShow()
}

/** axios 请求结束 */
export const doneLoading = () => {
  state.count = Math.max(0, state.count - 1)
  refreshProgress()
  if (state.count === 0 && navPhase) armSettle()
}

/** 路由开始切换（router.beforeEach 调用） */
export const beginNav = () => {
  navPhase = true
  routing = true
  if (settleTimer) {
    clearTimeout(settleTimer)
    settleTimer = null
  }
  refreshProgress()
  armShow()
}

/** 路由切换结束（router.afterEach / onError 调用，重定向多次也安全） */
export const endNav = () => {
  routing = false
  refreshProgress()
  armSettle()
}

/** 全部清空（异常兜底 / 强制复位） */
export const resetLoading = () => {
  state.count = 0
  routing = false
  navPhase = false
  if (showTimer) {
    clearTimeout(showTimer)
    showTimer = null
  }
  if (settleTimer) {
    clearTimeout(settleTimer)
    settleTimer = null
  }
  state.overlay = false
  finishProgress()
}

/** 暴露给组件订阅的只读状态 */
export const useGlobalLoader = () => state
