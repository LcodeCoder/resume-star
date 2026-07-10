/**
 * 前端应用入口文件
 * 功能：创建 Vue 应用并注册路由、Pinia、Element Plus 和全局样式
 */
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import App from './App.vue'
import router from './router'
import pinia from './store'
import { useUserStore } from './store/user'
import { initTheme } from './utils/theme'
import './style/index.css'

// 在挂载前应用主题，避免首屏闪烁
initTheme()

const app = createApp(App)

// 全局错误兜底：捕获组件渲染/生命周期/侦听器等未处理异常，记录日志并轻提示，
// 避免未捕获错误直接导致白屏。ErrorBoundary 负责渲染期的兜底 UI，这里覆盖
// 事件处理、异步回调等更广的场景。
app.config.errorHandler = (err, instance, info) => {
  // eslint-disable-next-line no-console
  console.error('[GlobalErrorHandler]', info, err)
  ElMessage.error('操作出现异常，请重试')
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)

const userStore = useUserStore()
// Render the shell immediately. Session restoration is background work and must
// never hold the first paint hostage to a slow or unavailable API.
app.mount('#app')
userStore.loadProfile()
