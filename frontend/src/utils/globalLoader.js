/**
 * 全局加载状态
 * 功能：统一管理顶部进度条与 Lcode 拼字遮罩；axios / router 都通过 start/done 接入。
 * 说明：用 reactive 暴露给组件订阅；count > 0 时进度条流动，overlayPending 大于 0 且持续 >250ms 时显示遮罩。
 */
import { reactive } from 'vue'

/**
 * 全局加载状态
 *  - count: 进行中的"轻量"任务数（决定顶部进度条是否流动）
 *  - overlay: 是否展示 Lcode 全屏遮罩（路由切换专用）
 *  - tick: 进度条每帧推进的 0~95 百分比（剩余 5% 留给完成动画）
 */
const state = reactive({
  count: 0,
  overlay: false,
  tick: 0
})

let progressTimer = null
let overlayTimer = null
let pendingOverlay = 0

/** 推进进度条：靠近 95% 时变慢，保证视觉一直在动 */
const stepProgress = () => {
  const remain = 95 - state.tick
  const step = Math.max(0.4, remain / 18)
  state.tick = Math.min(95, state.tick + step)
}

const startProgress = () => {
  if (progressTimer) return
  state.tick = Math.max(state.tick, 8)
  progressTimer = setInterval(stepProgress, 220)
}

const stopProgress = () => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
  // 跳到 100% 让进度条收尾，然后由组件监听 tick=0 复位
  state.tick = 100
  setTimeout(() => {
    if (state.count === 0) state.tick = 0
  }, 320)
}

/**
 * 开始一个加载任务
 * @param {object} options
 * @param {boolean} options.overlay 是否触发 Lcode 全屏遮罩（仅路由切换需要传 true）
 */
export const startLoading = ({ overlay = false } = {}) => {
  state.count += 1
  if (state.count === 1) startProgress()
  if (overlay) {
    pendingOverlay += 1
    // 250ms 内若已完成则不闪一下，避免瞬时切换抖动
    if (overlayTimer) clearTimeout(overlayTimer)
    overlayTimer = setTimeout(() => {
      if (pendingOverlay > 0) state.overlay = true
    }, 250)
  }
}

/**
 * 结束一个加载任务
 * @param {object} options
 * @param {boolean} options.overlay 是否对应一次 overlay 任务
 */
export const doneLoading = ({ overlay = false } = {}) => {
  state.count = Math.max(0, state.count - 1)
  if (overlay) {
    pendingOverlay = Math.max(0, pendingOverlay - 1)
    if (pendingOverlay === 0) {
      if (overlayTimer) {
        clearTimeout(overlayTimer)
        overlayTimer = null
      }
      state.overlay = false
    }
  }
  if (state.count === 0) stopProgress()
}

/** 全部清空（如登录跳转后） */
export const resetLoading = () => {
  state.count = 0
  pendingOverlay = 0
  if (overlayTimer) {
    clearTimeout(overlayTimer)
    overlayTimer = null
  }
  state.overlay = false
  stopProgress()
}

/** 暴露给组件订阅的只读状态 */
export const useGlobalLoader = () => state
