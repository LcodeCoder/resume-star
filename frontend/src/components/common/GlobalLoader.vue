<!--
  全局加载层
  功能：顶部 2px 渐变进度条 + 行星旋转全屏遮罩
  说明：订阅 globalLoader 的 tick/overlay；遮罩由导航阶段驱动并延续到首屏数据加载完成，
        overlay 已内置 220ms 延迟，瞬时切换不会闪，只有真正慢加载才显示行星动画。
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

  <!-- 慢加载遮罩：行星在中央自转，卫星沿轨道公转 -->
  <transition name="orbit-veil">
    <div v-if="state.overlay" class="orbit-veil" aria-live="polite" aria-busy="true">
      <div class="orbit-loader" aria-hidden="true">
        <span class="orbit-ring"></span>
        <span class="orbit-planet"></span>
        <span class="orbit-satellite"></span>
      </div>
      <p class="orbit-hint">正在校准航线…</p>
    </div>
  </transition>
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

/* 全屏遮罩：半透明 + 毛玻璃，让行星动画聚焦中央 */
.orbit-veil {
  position: fixed;
  inset: 0;
  z-index: 3900;
  display: grid;
  place-items: center;
  gap: 26px;
  grid-auto-flow: row;
  background: color-mix(in oklch, var(--space, #070b18), transparent 22%);
  backdrop-filter: blur(6px) saturate(120%);
  pointer-events: all;
}

/* 行星装置：轨道环 + 自转行星 + 公转卫星 */
.orbit-loader {
  position: relative;
  width: 96px;
  height: 96px;
}

/* 轨道环：淡淡的椭圆虚线，交代"星图"语境 */
.orbit-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1px dashed color-mix(in oklch, var(--accent, #7c97f5), transparent 55%);
  opacity: 0.7;
}

/* 主行星：径向渐变 + 光晕 + 缓慢自转的高光条 */
.orbit-planet {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 40px;
  height: 40px;
  margin: -20px 0 0 -20px;
  border-radius: 50%;
  background:
    radial-gradient(circle at 34% 30%,
      color-mix(in oklch, #ffffff, transparent 30%) 0%,
      #7c97f5 34%,
      #243a96 100%);
  box-shadow:
    0 0 0 1px color-mix(in oklch, #7c97f5, transparent 60%),
    0 0 26px color-mix(in oklch, #7c97f5, transparent 55%);
  overflow: hidden;
  animation: planet-spin 3.4s linear infinite;
}

/* 行星表面高光条：随自转扫过，强化"在转"的感知 */
.orbit-planet::after {
  content: '';
  position: absolute;
  top: -10%;
  left: 22%;
  width: 34%;
  height: 120%;
  background: color-mix(in oklch, #ffffff, transparent 78%);
  border-radius: 50%;
  filter: blur(2px);
}

/* 卫星：金色小星沿轨道公转 */
.orbit-satellite {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 96px;
  height: 96px;
  margin: -48px 0 0 -48px;
  animation: satellite-orbit 2.2s linear infinite;
}
.orbit-satellite::before {
  content: '';
  position: absolute;
  top: -4px;
  left: 50%;
  width: 8px;
  height: 8px;
  margin-left: -4px;
  border-radius: 50%;
  background: radial-gradient(circle at 40% 35%, #ffe9b8, #e6b35a);
  box-shadow: 0 0 12px color-mix(in oklch, #e6b35a, transparent 30%);
}

.orbit-hint {
  margin: 0;
  color: color-mix(in oklch, var(--ink, #e8ecf6), transparent 12%);
  font-size: 13px;
  letter-spacing: 0.08em;
  opacity: 0.9;
}

@keyframes planet-spin {
  to { transform: rotate(360deg); }
}
@keyframes satellite-orbit {
  to { transform: rotate(360deg); }
}

/* 遮罩淡入淡出 */
.orbit-veil-enter-active,
.orbit-veil-leave-active {
  transition: opacity 0.3s ease;
}
.orbit-veil-enter-from,
.orbit-veil-leave-to {
  opacity: 0;
}

/* 尊重减弱动态偏好：停自转/公转，保留静态行星与文案 */
@media (prefers-reduced-motion: reduce) {
  .orbit-planet,
  .orbit-satellite {
    animation: none;
  }
  .orbit-veil-enter-active,
  .orbit-veil-leave-active {
    transition: none;
  }
}
</style>
