<!--
  应用根组件
  功能：承载全局布局、路由视图，以及顶部进度条 + Lcode 拼字加载遮罩
  说明：路由视图外包一层 ErrorBoundary，单页渲染异常时降级为兜底 UI 而非整站白屏
-->
<script setup>
import GlobalLoader from './components/common/GlobalLoader.vue'
import ErrorBoundary from './components/common/ErrorBoundary.vue'
</script>

<template>
  <div class="starfield" aria-hidden="true"></div>
  <ErrorBoundary>
    <router-view />
  </ErrorBoundary>
  <GlobalLoader />
</template>
