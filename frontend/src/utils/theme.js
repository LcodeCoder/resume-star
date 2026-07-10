import { computed, ref } from 'vue'

const STORAGE_KEY = 'orbit_resume_theme'

export const THEME_OPTIONS = [
  { value: 'day-standard', label: '白昼标准', short: '昼' },
  { value: 'day-eye', label: '白昼护眼', short: '护' },
  { value: 'night-standard', label: '黑夜标准', short: '夜' },
  { value: 'night-eye', label: '黑夜护眼', short: '静' }
]

export const currentTheme = ref('day-standard')
export const isDark = computed(() => currentTheme.value.startsWith('night'))

export const applyTheme = (theme) => {
  const next = THEME_OPTIONS.some((item) => item.value === theme) ? theme : 'day-standard'
  currentTheme.value = next
  document.documentElement.dataset.theme = next
  document.documentElement.style.colorScheme = next.startsWith('night') ? 'dark' : 'light'
  try { localStorage.setItem(STORAGE_KEY, next) } catch (_) { /* storage may be disabled */ }
}

export const initTheme = () => {
  let saved = null
  try { saved = localStorage.getItem(STORAGE_KEY) } catch (_) { /* storage may be disabled */ }
  if (saved && THEME_OPTIONS.some((item) => item.value === saved)) return applyTheme(saved)
  const dark = window.matchMedia?.('(prefers-color-scheme: dark)').matches
  applyTheme(dark ? 'night-standard' : 'day-standard')
}

export const toggleTheme = () => {
  const index = THEME_OPTIONS.findIndex((item) => item.value === currentTheme.value)
  applyTheme(THEME_OPTIONS[(index + 1) % THEME_OPTIONS.length].value)
}
