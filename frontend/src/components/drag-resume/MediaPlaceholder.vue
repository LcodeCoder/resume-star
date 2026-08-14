<!--
  头像 / 图片 / 二维码的空态占位。
  画布与模板预览共用，避免「一行字 + 棋盘格」的廉价空盒。
-->
<script setup>
defineProps({
  kind: { type: String, required: true },
  label: { type: String, default: '' },
  compact: { type: Boolean, default: false }
})
</script>

<template>
  <span class="media-empty" :class="[`kind-${kind}`, { compact }]">
    <svg v-if="kind === 'avatar'" viewBox="0 0 48 48" aria-hidden="true">
      <circle cx="24" cy="18" r="8" fill="none" stroke="currentColor" stroke-width="2" />
      <path d="M10 40c2.4-8 8-12 14-12s11.6 4 14 12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
    </svg>
    <svg v-else-if="kind === 'qrcode'" viewBox="0 0 48 48" aria-hidden="true">
      <rect x="6" y="6" width="12" height="12" rx="1.5" fill="none" stroke="currentColor" stroke-width="2" />
      <rect x="9.5" y="9.5" width="5" height="5" fill="currentColor" />
      <rect x="30" y="6" width="12" height="12" rx="1.5" fill="none" stroke="currentColor" stroke-width="2" />
      <rect x="33.5" y="9.5" width="5" height="5" fill="currentColor" />
      <rect x="6" y="30" width="12" height="12" rx="1.5" fill="none" stroke="currentColor" stroke-width="2" />
      <rect x="9.5" y="33.5" width="5" height="5" fill="currentColor" />
      <rect x="22" y="22" width="5" height="5" fill="currentColor" />
      <rect x="30" y="22" width="5" height="5" fill="currentColor" />
      <rect x="22" y="30" width="5" height="5" fill="currentColor" />
      <rect x="38" y="30" width="4" height="4" fill="currentColor" />
      <rect x="30" y="38" width="12" height="4" fill="currentColor" />
    </svg>
    <svg v-else viewBox="0 0 48 48" aria-hidden="true">
      <rect x="7" y="11" width="34" height="26" rx="3" fill="none" stroke="currentColor" stroke-width="2" />
      <circle cx="17" cy="20" r="3" fill="none" stroke="currentColor" stroke-width="2" />
      <path d="M9 33l9-9 6 6 5-5 10 8" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
    </svg>
    <b v-if="label && !compact">{{ label }}</b>
  </span>
</template>

<style scoped>
.media-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  height: 100%;
  color: inherit;
  pointer-events: none;
}
.media-empty svg {
  width: 38%;
  height: 38%;
  max-width: 44px;
  max-height: 44px;
  flex: 0 0 auto;
}
.media-empty b {
  max-width: 86%;
  overflow: hidden;
  color: inherit;
  font-size: 11px;
  font-weight: 650;
  letter-spacing: .02em;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.media-empty.compact svg {
  width: 52%;
  height: 52%;
}
.media-empty.kind-qrcode svg {
  width: 46%;
  height: 46%;
}
</style>
