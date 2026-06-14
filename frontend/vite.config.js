import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * Vite 配置文件
 * 功能：配置 Vue 插件和本地开发代理，便于前端直接调用 Spring Boot `/api` 接口
 */
export default defineConfig({
  plugins: [vue()],
  build: {
    // 拆分第三方大依赖为独立 chunk：
    // - echarts/zrender 仅图表页（编辑器/后台）按需加载，且多页共享同一份
    // - element-plus 独立缓存，与业务代码解耦
    // - vue 全家桶（vue/router/pinia）合并为核心 chunk
    // 配合路由懒加载，首包从单包 2.4MB 降到「入口 + 布局 + 首页 + 核心库」。
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('echarts') || id.includes('zrender')) return 'echarts'
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'element-plus'
          if (id.includes('html-to-image')) return 'html-to-image'
          if (id.includes('/vue/') || id.includes('/@vue/') || id.includes('vue-router') || id.includes('pinia')) return 'vue-core'
          return 'vendor'
        }
      }
    }
  },
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
