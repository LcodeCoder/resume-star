<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  components: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false }
})

const emit = defineEmits(['update:title', 'update-component', 'add-text', 'export', 'save', 'import'])

const fields = computed(() =>
  (props.components || [])
    .map((item, index) => ({ item, index }))
    .filter(({ item }) => !item.hidden && ['text', 'contact', 'progress', 'rating', 'tag'].includes(item.type))
)

const fieldLabel = (item, index) => item.label || item.content?.slice(0, 8) || `段落 ${index + 1}`
</script>

<template>
  <section class="mobile-editor">
    <p class="mobile-lead">手机上改文字、导出即可。拖拽排版请用电脑。</p>
    <el-input
      :model-value="title"
      placeholder="简历标题"
      @update:model-value="emit('update:title', $event)"
    />
    <div class="mobile-actions">
      <el-button size="small" @click="emit('import')">导入</el-button>
      <el-button size="small" :loading="saving" @click="emit('save')">保存</el-button>
      <el-button size="small" type="primary" @click="emit('export', 'pdf-text')">导出文字版</el-button>
    </div>

    <article v-for="({ item, index }) in fields" :key="item.id" class="mobile-field">
      <label>{{ fieldLabel(item, index) }}</label>
      <el-input
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 8 }"
        :model-value="item.content"
        @update:model-value="emit('update-component', item.id, $event)"
      />
    </article>

    <el-button class="add-para" @click="emit('add-text')">增加一段文字</el-button>
    <el-empty v-if="!fields.length" description="还没有可改的文字，先导入或从示例开始" :image-size="64" />
  </section>
</template>

<style scoped>
.mobile-editor { display: grid; gap: 12px; margin-bottom: 14px; padding: 14px; border: 1px solid var(--line); border-radius: var(--radius-md); background: var(--surface); }
.mobile-lead { margin: 0; color: var(--ink-2); font-size: 13px; }
.mobile-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.mobile-field { display: grid; gap: 6px; }
.mobile-field label { color: var(--muted); font-size: 12px; font-weight: 700; }
.add-para { justify-self: start; }
</style>
