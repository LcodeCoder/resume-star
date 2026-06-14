<!--
  线条图标组件（描边风格，跟随字号与 currentColor）
  用法：<LineIcon name="mic" /> ，支持 mic / volume / headphones
-->
<script setup>
defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: '1em' }
})
</script>

<template>
  <svg
    class="line-icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.7"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <!-- 麦克风 -->
    <template v-if="name === 'mic'">
      <rect x="9" y="2.5" width="6" height="11" rx="3" />
      <path d="M5 11a7 7 0 0 0 14 0" />
      <line x1="12" y1="18" x2="12" y2="21.5" />
      <line x1="8.5" y1="21.5" x2="15.5" y2="21.5" />
    </template>
    <!-- 喇叭 / 音量 -->
    <template v-else-if="name === 'volume'">
      <path d="M4 9v6h3.5L13 19V5L7.5 9H4z" />
      <path d="M16.5 8.5a5 5 0 0 1 0 7" />
      <path d="M19 6a9 9 0 0 1 0 12" />
    </template>
    <!-- 耳机 -->
    <template v-else-if="name === 'headphones'">
      <path d="M4 14v-2a8 8 0 0 1 16 0v2" />
      <rect x="2.5" y="13.5" width="4" height="6.5" rx="1.6" />
      <rect x="17.5" y="13.5" width="4" height="6.5" rx="1.6" />
    </template>
  </svg>
</template>

<style scoped>
.line-icon {
  display: inline-block;
  vertical-align: -0.15em;
  flex-shrink: 0;
}
</style>
