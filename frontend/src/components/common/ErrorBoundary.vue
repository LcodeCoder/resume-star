<!--
  全局错误边界组件
  功能：用 onErrorCaptured 兜住子树（即整个路由视图）的渲染/生命周期异常，
        避免单个组件抛错导致整页白屏；出错时展示友好兜底 UI 并提供「重试 / 刷新」。
  说明：错误边界只负责「不白屏」，真正的 bug 仍需修复；这里同时把错误透传给
        全局 errorHandler（return false 阻止继续向上冒泡，避免重复处理）。
-->
<script setup>
import { ref, onErrorCaptured, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const error = ref(null)
const router = useRouter()
const route = useRoute()

onErrorCaptured((err, instance, info) => {
  // eslint-disable-next-line no-console
  console.error('[ErrorBoundary] 捕获到异常：', info, err)
  // 事件处理器（含 async 点击回调）里的异常，业务层/请求拦截器通常已用 ElMessage
  // 提示过（如「请先登录」「网络异常」）。这类异常不该把整页替换成兜底 UI，
  // 否则用户点个收藏、导出失败都会跳到错误页，体验很差。吞掉冒泡即可。
  if (info && info.includes('event handler')) {
    return false
  }
  // 只有渲染 / 生命周期等真正会导致白屏的异常，才展示兜底页
  error.value = err
  return false
})

// 路由切换后自动清除错误态：跳到其它页面时不该再卡在上一页的报错界面
watch(() => route.fullPath, () => {
  error.value = null
})

/** 重试：清空错误态，让子树重新渲染当前页面 */
function retry() {
  error.value = null
}

/** 回首页：渲染错误往往与当前路由数据相关，回到安全页面 */
function goHome() {
  error.value = null
  router.push('/').catch(() => {})
}
</script>

<template>
  <div v-if="error" class="error-boundary">
    <div class="error-card">
      <div class="error-icon">⚠️</div>
      <h2 class="error-title">页面出了点问题</h2>
      <p class="error-desc">这部分内容加载失败了，你可以重试，或返回首页继续使用。</p>
      <div class="error-actions">
        <button class="eb-btn eb-btn--primary" @click="retry">重试</button>
        <button class="eb-btn" @click="goHome">返回首页</button>
      </div>
    </div>
  </div>
  <slot v-else />
</template>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 24px;
}
.error-card {
  max-width: 420px;
  text-align: center;
  padding: 40px 32px;
  background: var(--color-surface, #fff);
  border: 1px solid var(--color-border, #e8e8ed);
  border-radius: var(--radius-xl, 16px);
  box-shadow: var(--shadow-md, 0 6px 20px rgba(0, 0, 0, 0.08));
}
.error-icon {
  font-size: 40px;
  line-height: 1;
  margin-bottom: 16px;
}
.error-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text, #1d1d1f);
}
.error-desc {
  margin: 0 0 24px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-muted, #6e6e73);
}
.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
.eb-btn {
  padding: 9px 20px;
  font-size: 14px;
  border: 1px solid var(--color-border, #e8e8ed);
  border-radius: var(--radius-md, 8px);
  background: var(--color-surface, #fff);
  color: var(--color-text-tertiary, #424245);
  cursor: pointer;
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.eb-btn:hover {
  transform: translateY(-1px);
}
.eb-btn--primary {
  background: var(--color-primary, #5b5bd6);
  border-color: var(--color-primary, #5b5bd6);
  color: #fff;
}
</style>
