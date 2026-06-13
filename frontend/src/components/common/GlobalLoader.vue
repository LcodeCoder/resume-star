<!--
  全局加载层
  功能：顶部 2px 渐变进度条 + Lcode 拼字全屏遮罩
  说明：订阅 globalLoader 的 tick/overlay；遮罩仅在 250ms 以上的任务中显示，进度条始终展示
-->
<script setup>
import { computed } from 'vue'
import { useGlobalLoader } from '../../utils/globalLoader'

const state = useGlobalLoader()

const barStyle = computed(() => ({
  width: state.tick + '%',
  opacity: state.tick > 0 && state.tick < 100 ? 1 : (state.tick >= 100 ? 0.85 : 0)
}))

/** Lcode 五个字母依次出现，再循环淡出 */
const LETTERS = ['L', 'c', 'o', 'd', 'e']
</script>

<template>
  <!-- 顶部进度条：始终常驻，根据 tick 控制宽度 -->
  <div class="global-progress-bar" :style="barStyle"></div>

  <!-- Lcode 拼字全屏遮罩：仅长任务显示 -->
  <transition name="lcode-fade">
    <div v-if="state.overlay" class="lcode-overlay" role="status" aria-live="polite">
      <div class="lcode-stage">
        <span
          v-for="(letter, idx) in LETTERS"
          :key="letter"
          class="lcode-letter"
          :class="`lcode-letter-${idx}`"
          :style="{ animationDelay: idx * 0.1 + 's' }"
        >{{ letter }}</span>
      </div>
      <p class="lcode-tip">页面加载中…</p>
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
  background: linear-gradient(90deg, #5b5bd6 0%, #6f6fe0 50%, #b06ed0 100%);
  box-shadow: 0 0 8px rgba(91, 91, 214, 0.45);
  z-index: 4000;
  transition: width 0.22s ease, opacity 0.32s ease;
  pointer-events: none;
}

/* Lcode 遮罩：毛玻璃 + 居中卡片 */
.lcode-overlay {
  position: fixed;
  inset: 0;
  z-index: 3900;
  background: rgba(245, 245, 248, 0.78);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
}

.lcode-stage {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-family: 'JetBrains Mono', 'SF Mono', Menlo, ui-monospace, monospace;
  font-weight: 700;
  font-size: 56px;
  letter-spacing: 2px;
}

.lcode-letter {
  display: inline-block;
  opacity: 0;
  transform: translateY(14px) scale(0.92);
  background: linear-gradient(135deg, #5b5bd6, #b06ed0);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  animation: lcode-pop 1.6s ease-in-out infinite;
}

/* 让首字母 L 略大、稍下沉，模拟「品牌字标」节奏 */
.lcode-letter-0 {
  font-size: 64px;
  background: linear-gradient(135deg, #4b4bcc, #5b5bd6);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.lcode-tip {
  margin: 0;
  font-size: 13px;
  color: #6e6e73;
  letter-spacing: 1px;
}

/* 拼字动画：先弹入、稳一会、再淡出，循环 */
@keyframes lcode-pop {
  0%   { opacity: 0; transform: translateY(14px) scale(0.92); }
  18%  { opacity: 1; transform: translateY(0) scale(1); }
  62%  { opacity: 1; transform: translateY(0) scale(1); }
  100% { opacity: 0; transform: translateY(-8px) scale(0.96); }
}

/* 遮罩本身的淡入淡出 */
.lcode-fade-enter-active,
.lcode-fade-leave-active {
  transition: opacity 0.22s ease;
}
.lcode-fade-enter-from,
.lcode-fade-leave-to {
  opacity: 0;
}
</style>
