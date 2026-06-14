<!--
  会员高级可视化组件数据编辑器（侧滑抽屉）
  页面位置：编辑器选中图表组件后，点工具栏「编辑图表数据」或双击图表打开
  功能：按图表类型提供结构化数据编辑——雷达维度 / 环形百分比 / 仪表盘 / 时间线条目 /
        词云词条 / 柱状项 / 统计卡项，支持增删行、改数值、改配色；
        直接修改 component.style，复用编辑器既有深度监听自动保存。
-->
<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** 抽屉显隐 */
  visible: Boolean,
  /** 当前选中的可视化组件对象（直接修改其 style 字段） */
  component: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:visible'])

const close = () => emit('update:visible', false)

/** 当前组件类型 */
const type = computed(() => props.component?.type || '')

/** 保证 style 对象存在并返回 */
const ensureStyle = () => {
  if (!props.component.style) props.component.style = {}
  return props.component.style
}

/** 保证 style 下某个数组字段存在，不存在则初始化为空数组 */
const ensureArray = (key) => {
  const style = ensureStyle()
  if (!Array.isArray(style[key])) style[key] = []
  return style[key]
}

/* ===== 各类型数据数组（响应式直连 style） ===== */
const indicators = computed(() => (props.component ? ensureArray('indicators') : []))
const items = computed(() => (props.component ? ensureArray('items') : []))
const words = computed(() => (props.component ? ensureArray('words') : []))
const bars = computed(() => (props.component ? ensureArray('bars') : []))
const stats = computed(() => (props.component ? ensureArray('stats') : []))

/* ===== 增删行 ===== */
const addIndicator = () => indicators.value.push({ name: '新维度', value: 60 })
const addItem = () => items.value.push({ time: '2024.01 - 2024.12', title: '新条目', desc: '描述' })
const addWord = () => words.value.push({ text: '关键词', weight: 2 })
const addBar = () => bars.value.push({ label: '新项目', value: 60 })
const addStat = () => stats.value.push({ value: '0', label: '新指标' })
const removeAt = (list, index) => list.splice(index, 1)

/** 标题映射 */
const typeLabel = computed(() => ({
  radar: '雷达图维度',
  ring: '环形能力图',
  gauge: '仪表盘评分',
  timeline: '时间线条目',
  wordcloud: '词云词条',
  barchart: '柱状能力项',
  statcard: '数据统计项'
}[type.value] || '图表数据'))
</script>

<template>
  <el-drawer
    :model-value="props.visible"
    :title="`编辑${typeLabel}`"
    size="380px"
    @close="close"
  >
    <div v-if="component" class="visual-editor">
      <!-- 通用：配色 -->
      <div class="ve-section">
        <div class="ve-section-title">配色</div>
        <div class="ve-color-row">
          <span class="ve-label">主色</span>
          <el-color-picker v-model="component.style.color" size="small" />
        </div>
        <div v-if="['ring', 'gauge', 'timeline', 'barchart'].includes(type)" class="ve-color-row">
          <span class="ve-label">{{ type === 'timeline' ? '轴线色' : '底色' }}</span>
          <el-color-picker v-model="component.style.background" size="small" />
        </div>
        <div class="ve-color-row">
          <span class="ve-label">文字色</span>
          <el-color-picker v-model="component.style.textColor" size="small" />
        </div>
      </div>

      <!-- 环形 / 仪表盘：百分比 + 标题 -->
      <div v-if="type === 'ring' || type === 'gauge'" class="ve-section">
        <div class="ve-section-title">数值</div>
        <div class="ve-field">
          <span class="ve-label">标题</span>
          <el-input v-model="component.content" size="small" placeholder="如 综合能力" />
        </div>
        <div class="ve-field">
          <span class="ve-label">百分比</span>
          <el-slider v-model="component.style.percent" :min="0" :max="100" show-input size="small" />
        </div>
        <div class="ve-field">
          <span class="ve-label">环宽</span>
          <el-input-number v-model="component.style.ringWidth" :min="4" :max="40" size="small" />
        </div>
      </div>

      <!-- 雷达图维度 -->
      <div v-if="type === 'radar'" class="ve-section">
        <div class="ve-section-title">
          维度（{{ indicators.length }}）
          <el-button size="small" type="primary" plain @click="addIndicator">+ 添加</el-button>
        </div>
        <div v-for="(it, idx) in indicators" :key="idx" class="ve-row">
          <el-input v-model="it.name" size="small" placeholder="维度名" style="flex: 1" />
          <el-input-number v-model="it.value" :min="0" :max="100" size="small" controls-position="right" style="width: 96px" />
          <button class="ve-del" :disabled="indicators.length <= 3" title="至少 3 个维度" @click="removeAt(indicators, idx)">×</button>
        </div>
        <p class="ve-hint">雷达图至少保留 3 个维度。</p>
      </div>

      <!-- 时间线条目 -->
      <div v-if="type === 'timeline'" class="ve-section">
        <div class="ve-section-title">
          条目（{{ items.length }}）
          <el-button size="small" type="primary" plain @click="addItem">+ 添加</el-button>
        </div>
        <div v-for="(it, idx) in items" :key="idx" class="ve-card">
          <div class="ve-card-head">
            <span>#{{ idx + 1 }}</span>
            <button class="ve-del" @click="removeAt(items, idx)">×</button>
          </div>
          <el-input v-model="it.time" size="small" placeholder="时间，如 2022.07 - 至今" />
          <el-input v-model="it.title" size="small" placeholder="标题，如 某公司 · 职位" style="margin-top: 6px" />
          <el-input v-model="it.desc" size="small" placeholder="描述（可选）" style="margin-top: 6px" />
        </div>
      </div>

      <!-- 词云词条 -->
      <div v-if="type === 'wordcloud'" class="ve-section">
        <div class="ve-section-title">
          词条（{{ words.length }}）
          <el-button size="small" type="primary" plain @click="addWord">+ 添加</el-button>
        </div>
        <div v-for="(w, idx) in words" :key="idx" class="ve-row">
          <el-input v-model="w.text" size="small" placeholder="词" style="flex: 1" />
          <el-select v-model="w.weight" size="small" style="width: 90px" placeholder="权重">
            <el-option :value="1" label="小" />
            <el-option :value="2" label="中" />
            <el-option :value="3" label="大" />
          </el-select>
          <button class="ve-del" @click="removeAt(words, idx)">×</button>
        </div>
        <p class="ve-hint">权重越大字号越大。</p>
      </div>

      <!-- 柱状能力项 -->
      <div v-if="type === 'barchart'" class="ve-section">
        <div class="ve-section-title">
          条目（{{ bars.length }}）
          <el-button size="small" type="primary" plain @click="addBar">+ 添加</el-button>
        </div>
        <div v-for="(b, idx) in bars" :key="idx" class="ve-row">
          <el-input v-model="b.label" size="small" placeholder="名称" style="flex: 1" />
          <el-input-number v-model="b.value" :min="0" :max="100" size="small" controls-position="right" style="width: 96px" />
          <button class="ve-del" @click="removeAt(bars, idx)">×</button>
        </div>
      </div>

      <!-- 数据统计项 -->
      <div v-if="type === 'statcard'" class="ve-section">
        <div class="ve-section-title">
          指标（{{ stats.length }}）
          <el-button size="small" type="primary" plain @click="addStat">+ 添加</el-button>
        </div>
        <div v-for="(s, idx) in stats" :key="idx" class="ve-row">
          <el-input v-model="s.value" size="small" placeholder="数值，如 5+" style="width: 100px" />
          <el-input v-model="s.label" size="small" placeholder="说明，如 年经验" style="flex: 1" />
          <button class="ve-del" @click="removeAt(stats, idx)">×</button>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button type="primary" @click="close">完成</el-button>
    </template>
  </el-drawer>
</template>

<style scoped>
.visual-editor {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.ve-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ve-section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text);
  padding-bottom: 6px;
  border-bottom: 1px solid #ececec;
}
.ve-color-row,
.ve-field {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ve-field {
  flex-wrap: wrap;
}
.ve-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  width: 56px;
  flex-shrink: 0;
}
.ve-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ve-card {
  background: #f5f5f7;
  border-radius: 8px;
  padding: 10px;
}
.ve-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
}
.ve-del {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 6px;
  background: #fde7e6;
  color: #d82c20;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  flex-shrink: 0;
}
.ve-del:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.ve-hint {
  font-size: 12px;
  color: #9e9ea4;
  margin: 0;
}

/* ===== 暗色模式：分隔线 / 卡片底 ===== */
html.dark .ve-section-title {
  border-bottom-color: rgba(255, 255, 255, 0.1);
}

html.dark .ve-card {
  background: var(--color-elevated);
}
</style>
