/**
 * 主题（明/暗）管理
 * 功能：在 <html> 上切换 `dark` 类，驱动 Element Plus 暗色 css-vars 与本项目 html.dark 语义色覆盖；
 *       选择持久化到 localStorage，未选择时跟随系统偏好。
 */
import { ref } from 'vue'

const STORAGE_KEY = 'resume_lcode_theme'

/** 当前是否暗色（全局响应式，供组件读取） */
export const isDark = ref(false)

/** 应用主题：切换 html.dark 类并持久化 */
export const applyTheme = (dark) => {
  isDark.value = dark
  document.documentElement.classList.toggle('dark', dark)
  try {
    localStorage.setItem(STORAGE_KEY, dark ? 'dark' : 'light')
  } catch (e) {
    /* localStorage 不可用时忽略 */
  }
}

/** 初始化主题：优先用户选择，其次系统偏好 */
export const initTheme = () => {
  let saved = null
  try {
    saved = localStorage.getItem(STORAGE_KEY)
  } catch (e) {
    saved = null
  }
  const dark = saved
    ? saved === 'dark'
    : !!window.matchMedia?.('(prefers-color-scheme: dark)').matches
  applyTheme(dark)
}

/** 切换明暗 */
export const toggleTheme = () => applyTheme(!isDark.value)
