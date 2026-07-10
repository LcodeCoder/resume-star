<script setup>
import { ref } from 'vue'
import { THEME_OPTIONS, applyTheme, currentTheme } from '../../utils/theme'

defineProps({ compact: { type: Boolean, default: false } })
const open = ref(false)

const choose = (value) => {
  applyTheme(value)
  open.value = false
}
</script>

<template>
  <div class="theme-control" :class="{ compact }">
    <button class="theme-trigger" type="button" :aria-expanded="open" aria-label="切换显示主题" @click="open = !open">
      <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="3"/><circle cx="12" cy="12" r="8"/><path d="M12 1v3M12 20v3M1 12h3M20 12h3"/></svg>
      <span v-if="!compact">{{ THEME_OPTIONS.find(item => item.value === currentTheme)?.label }}</span>
    </button>
    <div v-if="open" class="theme-menu" role="menu">
      <div class="theme-menu-title">阅读环境</div>
      <button
        v-for="option in THEME_OPTIONS"
        :key="option.value"
        type="button"
        role="menuitemradio"
        :aria-checked="currentTheme === option.value"
        :class="{ active: currentTheme === option.value }"
        @click="choose(option.value)"
      >
        <span class="theme-swatch" :data-swatch="option.value">{{ option.short }}</span>
        <span>{{ option.label }}</span>
        <svg v-if="currentTheme === option.value" viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4 4L19 6"/></svg>
      </button>
    </div>
  </div>
</template>
