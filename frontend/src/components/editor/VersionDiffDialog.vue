<script setup>
import { computed } from 'vue'
import { diffResumeComponents } from '../../utils/resumeDiff'

const props = defineProps({
  visible: { type: Boolean, default: false },
  current: { type: Array, default: () => [] },
  previous: { type: Array, default: () => [] },
  previousTitle: { type: String, default: '历史版本' }
})

const emit = defineEmits(['update:visible'])

const diff = computed(() => diffResumeComponents(props.current, props.previous))
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="版本对比"
    width="560px"
    @update:model-value="emit('update:visible', $event)"
  >
    <p class="diff-lead">当前内容 对比 {{ previousTitle }}</p>
    <p v-if="!diff.total" class="diff-empty">这两份内容没有可识别的差异。</p>
    <div v-else class="diff-body">
      <section v-if="diff.changed.length">
        <h3>修改 {{ diff.changed.length }}</h3>
        <article v-for="item in diff.changed" :key="`c-${item.id}`">
          <strong>{{ item.label }}</strong>
          <ul>
            <li v-for="field in item.fields" :key="field.field">
              <span>{{ field.field }}</span>
              <em>{{ field.before || '空' }}</em>
              →
              <b>{{ field.after || '空' }}</b>
            </li>
          </ul>
        </article>
      </section>
      <section v-if="diff.added.length">
        <h3>新增 {{ diff.added.length }}</h3>
        <p v-for="item in diff.added" :key="`a-${item.id}`">{{ item.label }}：{{ item.content || '（无文字）' }}</p>
      </section>
      <section v-if="diff.removed.length">
        <h3>删除 {{ diff.removed.length }}</h3>
        <p v-for="item in diff.removed" :key="`r-${item.id}`">{{ item.label }}：{{ item.content || '（无文字）' }}</p>
      </section>
    </div>
  </el-dialog>
</template>

<style scoped>
.diff-lead { margin: 0 0 12px; color: var(--ink-2); font-size: 13px; }
.diff-empty { margin: 0; color: var(--muted); }
.diff-body { display: grid; gap: 16px; max-height: 60vh; overflow: auto; }
.diff-body h3 { margin: 0 0 8px; font-size: 13px; }
.diff-body article, .diff-body p { margin: 0 0 8px; font-size: 13px; color: var(--ink-2); }
.diff-body ul { margin: 6px 0 0; padding-left: 18px; }
.diff-body li { margin: 0 0 4px; }
.diff-body em { color: var(--muted); font-style: normal; }
.diff-body b { color: var(--ink); font-weight: 650; }
</style>
