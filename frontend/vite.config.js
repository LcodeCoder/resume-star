import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * Vite 配置文件
 * 功能：配置 Vue 插件和本地开发代理，便于前端直接调用 Spring Boot `/api` 接口
 */
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // 项目位于外置卷（/Volumes），fsevents 文件监听不可靠，改用轮询保证热更新生效
    watch: {
      usePolling: true,
      interval: 300
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
