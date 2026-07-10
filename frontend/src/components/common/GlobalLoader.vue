<!--
  全局加载层
  功能：顶部 2px 渐变进度条 + Lcode 拼字全屏遮罩
  说明：订阅 globalLoader 的 tick/overlay；遮罩由导航阶段驱动并延续到首屏数据加载完成，进度条始终展示
-->
<script setup>
import { computed } from 'vue'
import { useGlobalLoader } from '../../utils/globalLoader'

const state = useGlobalLoader()

const barStyle = computed(() => ({
  width: state.tick + '%',
  opacity: state.tick > 0 && state.tick < 100 ? 1 : (state.tick >= 100 ? 0.85 : 0)
}))

</script>

<template>
  <!-- 顶部进度条：始终常驻，根据 tick 控制宽度 -->
  <div class="global-progress-bar" :style="barStyle"></div>

</template>

<style scoped>
/* 顶部进度条：渐变 + 阴影呼应导航 */
.global-progress-bar {
  position: fixed;
  top: 0;
  left: 0;
  height: 2px;
  background: var(--accent);
  z-index: 4000;
  transition: width 0.22s ease, opacity 0.32s ease;
  pointer-events: none;
}

</style>
