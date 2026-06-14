/**
 * 前端应用入口文件
 * 功能：创建 Vue 应用并注册路由、Pinia、Element Plus 和全局样式
 */
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
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
app.use(pinia)
app.use(router)
app.use(ElementPlus)

const userStore = useUserStore()
userStore.loadProfile().finally(() => {
  app.mount('#app')
})
