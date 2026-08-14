<script setup>
import TemplatePreview from '../template-preview/TemplatePreview.vue'

defineProps({
  templates: { type: Array, default: () => [] },
  compact: { type: Boolean, default: false }
})

const emit = defineEmits(['apply-template', 'apply-example', 'import', 'blank'])
</script>

<template>
  <div class="start-from" :class="{ compact }">
    <header>
      <h2>先选一种开始方式</h2>
      <p>空白画布最慢。套模板、用示例或导入旧简历，都能更快做出一页能投的内容。</p>
    </header>

    <div class="start-actions">
      <button type="button" class="start-card" @click="emit('apply-example')">
        <strong>从示例开始</strong>
        <span>一页完整简历，改姓名和经历就能用</span>
      </button>
      <button type="button" class="start-card" @click="emit('import')">
        <strong>导入旧简历</strong>
        <span>支持 PDF、Word（.docx）、TXT</span>
      </button>
      <button type="button" class="start-card ghost" @click="emit('blank')">
        <strong>空白开始</strong>
        <span>从组件库自己搭</span>
      </button>
    </div>

    <div v-if="templates.length" class="start-templates">
      <h3>或套用模板</h3>
      <div class="template-row">
        <button
          v-for="item in templates.slice(0, compact ? 4 : 6)"
          :key="item.id"
          type="button"
          class="template-pick"
          @click="emit('apply-template', item)"
        >
          <TemplatePreview :components="item.components" :page-style="item.style" size="compact" />
          <span>{{ item.name }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.start-from { width: min(100%, 640px); padding: 22px 22px 18px; border: 1px solid var(--line); border-radius: var(--radius-md); background: var(--surface); box-shadow: var(--shadow); pointer-events: auto; }
.start-from header h2 { margin: 0 0 6px; font-size: 20px; letter-spacing: -.02em; }
.start-from header p { margin: 0 0 16px; color: var(--ink-2); font-size: 13px; line-height: 1.6; }
.start-actions { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.start-card { display: grid; gap: 4px; min-height: 86px; padding: 12px; border: 1px solid var(--line); border-radius: var(--radius-sm); background: var(--surface-2); text-align: left; cursor: pointer; }
.start-card:hover, .template-pick:hover { border-color: var(--accent); }
.start-card strong { font-size: 14px; }
.start-card span { color: var(--muted); font-size: 12px; line-height: 1.45; }
.start-card.ghost { background: transparent; }
.start-templates { margin-top: 16px; }
.start-templates h3 { margin: 0 0 8px; color: var(--muted); font-size: 12px; font-weight: 700; }
.template-row { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.template-pick { display: grid; gap: 6px; padding: 6px; border: 1px solid var(--line); border-radius: var(--radius-sm); background: var(--surface); cursor: pointer; }
.template-pick span { overflow: hidden; color: var(--ink-2); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.start-from.compact .start-actions, .start-from.compact .template-row { grid-template-columns: 1fr 1fr; }
@media (max-width: 720px) {
  .start-from { padding: 16px 14px; }
  .start-actions, .template-row { grid-template-columns: 1fr; }
}
</style>
