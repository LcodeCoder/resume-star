<!--
  简历可视化组件渲染器（会员高级组件）
  功能：统一渲染雷达图 / 环形能力图 / 仪表盘 / 时间线 / 词云 / 柱状能力图 / 数据统计卡
  说明：全部用内联 SVG / HTML 实现，不依赖 canvas，保证 PDF 打印、PNG(html-to-image)、
        Word(outerHTML) 三种导出都能正确呈现；与画布缩放天然兼容（矢量）。
  数据约定：图表数据存于 component.style 下的专用字段（indicators / percent / items / words / bars / stats），
            后台/编辑器可改，缺省时回退到内置示例数据。
-->
<script setup>
import { computed } from 'vue'

const props = defineProps({
  component: {
    type: Object,
    required: true
  }
})

const style = computed(() => props.component.style || {})
const accent = computed(() => style.value.color || '#0071e3')
const trackColor = computed(() => style.value.background || '#e8eef5')
const textColor = computed(() => style.value.textColor || '#1d1d1f')

/* ===================== 雷达图 ===================== */
/** 雷达维度：[{ name, value(0-100) }] */
const radarIndicators = computed(() => {
  const list = style.value.indicators
  if (Array.isArray(list) && list.length >= 3) return list
  return [
    { name: '专业技能', value: 90 },
    { name: '沟通协作', value: 75 },
    { name: '学习能力', value: 85 },
    { name: '项目管理', value: 70 },
    { name: '创新思维', value: 80 },
    { name: '抗压能力', value: 78 }
  ]
})

/** 计算极坐标点：i 为第几个轴，ratio 为半径比例 0-1 */
const radarPoint = (cx, cy, r, index, total, ratio) => {
  const angle = (Math.PI * 2 * index) / total - Math.PI / 2
  return [cx + r * ratio * Math.cos(angle), cy + r * ratio * Math.sin(angle)]
}

const radarGeometry = computed(() => {
  const cx = 140
  const cy = 130
  const r = 96
  const items = radarIndicators.value
  const total = items.length
  const rings = [0.25, 0.5, 0.75, 1]
  // 同心多边形网格
  const grids = rings.map((ratio) =>
    items.map((_, i) => radarPoint(cx, cy, r, i, total, ratio).map((n) => n.toFixed(1)).join(',')).join(' ')
  )
  // 轴线
  const axes = items.map((_, i) => {
    const [x, y] = radarPoint(cx, cy, r, i, total, 1)
    return { x2: x.toFixed(1), y2: y.toFixed(1) }
  })
  // 数据多边形
  const dataPts = items
    .map((it, i) => radarPoint(cx, cy, r, i, total, Math.max(0, Math.min(100, it.value || 0)) / 100))
  const dataPolygon = dataPts.map((p) => p.map((n) => n.toFixed(1)).join(',')).join(' ')
  // 标签位置（略微外扩）
  const labels = items.map((it, i) => {
    const [x, y] = radarPoint(cx, cy, r + 18, i, total, 1)
    let anchor = 'middle'
    if (x < cx - 4) anchor = 'end'
    else if (x > cx + 4) anchor = 'start'
    return { x: x.toFixed(1), y: y.toFixed(1), anchor, name: it.name, value: it.value }
  })
  return { cx, cy, grids, axes, dataPolygon, dataPts, labels }
})

/* ===================== 环形能力图 ===================== */
const ringPercent = computed(() => Math.max(0, Math.min(100, style.value.percent ?? 75)))
const ringGeometry = computed(() => {
  const size = 120
  const stroke = style.value.ringWidth || 14
  const r = (size - stroke) / 2
  const c = 2 * Math.PI * r
  return {
    size,
    stroke,
    r,
    center: size / 2,
    circumference: c,
    dash: (c * ringPercent.value) / 100
  }
})

/* ===================== 仪表盘（半环） ===================== */
const gaugePercent = computed(() => Math.max(0, Math.min(100, style.value.percent ?? 68)))
const gaugeGeometry = computed(() => {
  // 半圆弧：从左(180°)到右(0°)
  const w = 200
  const cx = w / 2
  const cy = 96
  const r = 80
  const stroke = style.value.ringWidth || 16
  const start = { x: cx - r, y: cy }
  const end = { x: cx + r, y: cy }
  // 进度终点角度
  const angle = Math.PI - Math.PI * (gaugePercent.value / 100)
  const px = cx + r * Math.cos(angle)
  const py = cy - r * Math.sin(angle)
  const largeArc = gaugePercent.value > 50 ? 1 : 0
  return {
    w,
    cy,
    stroke,
    bgPath: `M ${start.x} ${start.y} A ${r} ${r} 0 0 1 ${end.x} ${end.y}`,
    valuePath: `M ${start.x} ${start.y} A ${r} ${r} 0 ${largeArc} 1 ${px.toFixed(1)} ${py.toFixed(1)}`
  }
})

/* ===================== 时间线 / 履历轴 ===================== */
const timelineItems = computed(() => {
  const list = style.value.items
  if (Array.isArray(list) && list.length) return list
  return [
    { time: '2022.07 - 至今', title: '某科技公司 · 高级前端', desc: '负责核心业务架构与团队协作' },
    { time: '2020.06 - 2022.06', title: '某互联网公司 · 前端工程师', desc: '主导多个 C 端项目从 0 到 1' },
    { time: '2016.09 - 2020.06', title: '某大学 · 计算机科学与技术', desc: '主修数据结构、操作系统、网络' }
  ]
})

/* ===================== 词云 / 标签云 ===================== */
const wordItems = computed(() => {
  const list = style.value.words
  if (Array.isArray(list) && list.length) return list
  return [
    { text: 'JavaScript', weight: 3 }, { text: 'Vue', weight: 3 }, { text: 'TypeScript', weight: 2 },
    { text: 'Node.js', weight: 2 }, { text: 'Webpack', weight: 1 }, { text: '性能优化', weight: 2 },
    { text: '工程化', weight: 1 }, { text: 'React', weight: 2 }, { text: 'CSS', weight: 1 },
    { text: '架构设计', weight: 3 }, { text: 'Git', weight: 1 }, { text: '团队协作', weight: 2 }
  ]
})
const WORD_PALETTE = ['#0071e3', '#34c759', '#ff9500', '#af52de', '#ff3b30', '#5856d6', '#1d1d1f']
const wordStyle = (word, index) => ({
  fontSize: `${11 + (word.weight || 1) * 5}px`,
  fontWeight: (word.weight || 1) >= 3 ? 700 : 500,
  color: WORD_PALETTE[index % WORD_PALETTE.length]
})

/* ===================== 柱状能力图 ===================== */
const barItems = computed(() => {
  const list = style.value.bars
  if (Array.isArray(list) && list.length) return list
  return [
    { label: '后端开发', value: 90 },
    { label: '前端开发', value: 80 },
    { label: '数据库', value: 75 },
    { label: '运维部署', value: 65 }
  ]
})

/* ===================== 数据统计卡 ===================== */
const statItems = computed(() => {
  const list = style.value.stats
  if (Array.isArray(list) && list.length) return list
  return [
    { value: '5+', label: '年经验' },
    { value: '20+', label: '项目' },
    { value: '99.9%', label: '可用性' }
  ]
})
</script>

<template>
  <!-- 雷达图 -->
  <svg
    v-if="component.type === 'radar'"
    class="visual-svg"
    viewBox="0 0 280 250"
    preserveAspectRatio="xMidYMid meet"
  >
    <polygon
      v-for="(grid, i) in radarGeometry.grids"
      :key="`g-${i}`"
      :points="grid"
      fill="none"
      :stroke="i === radarGeometry.grids.length - 1 ? '#c9d2dd' : '#e6ebf2'"
      stroke-width="1"
    />
    <line
      v-for="(axis, i) in radarGeometry.axes"
      :key="`a-${i}`"
      :x1="radarGeometry.cx"
      :y1="radarGeometry.cy"
      :x2="axis.x2"
      :y2="axis.y2"
      stroke="#e6ebf2"
      stroke-width="1"
    />
    <polygon :points="radarGeometry.dataPolygon" :fill="accent" fill-opacity="0.18" :stroke="accent" stroke-width="2" />
    <circle
      v-for="(p, i) in radarGeometry.dataPts"
      :key="`p-${i}`"
      :cx="p[0].toFixed(1)"
      :cy="p[1].toFixed(1)"
      r="3"
      :fill="accent"
    />
    <text
      v-for="(label, i) in radarGeometry.labels"
      :key="`l-${i}`"
      :x="label.x"
      :y="label.y"
      :text-anchor="label.anchor"
      dominant-baseline="middle"
      :fill="textColor"
      font-size="11"
      font-weight="500"
    >{{ label.name }}</text>
  </svg>

  <!-- 环形能力图 -->
  <div v-else-if="component.type === 'ring'" class="visual-ring">
    <svg :viewBox="`0 0 ${ringGeometry.size} ${ringGeometry.size}`" class="visual-ring-svg">
      <circle
        :cx="ringGeometry.center"
        :cy="ringGeometry.center"
        :r="ringGeometry.r"
        fill="none"
        :stroke="trackColor"
        :stroke-width="ringGeometry.stroke"
      />
      <circle
        :cx="ringGeometry.center"
        :cy="ringGeometry.center"
        :r="ringGeometry.r"
        fill="none"
        :stroke="accent"
        :stroke-width="ringGeometry.stroke"
        stroke-linecap="round"
        :stroke-dasharray="`${ringGeometry.dash} ${ringGeometry.circumference}`"
        :transform="`rotate(-90 ${ringGeometry.center} ${ringGeometry.center})`"
      />
      <text
        :x="ringGeometry.center"
        :y="ringGeometry.center"
        text-anchor="middle"
        dominant-baseline="central"
        :fill="textColor"
        font-size="22"
        font-weight="700"
      >{{ ringPercent }}%</text>
    </svg>
    <span class="visual-ring-label" :style="{ color: textColor }">{{ component.content || '能力指数' }}</span>
  </div>

  <!-- 仪表盘评分 -->
  <div v-else-if="component.type === 'gauge'" class="visual-gauge">
    <svg :viewBox="`0 0 ${gaugeGeometry.w} 120`" class="visual-gauge-svg">
      <path :d="gaugeGeometry.bgPath" fill="none" :stroke="trackColor" :stroke-width="gaugeGeometry.stroke" stroke-linecap="round" />
      <path :d="gaugeGeometry.valuePath" fill="none" :stroke="accent" :stroke-width="gaugeGeometry.stroke" stroke-linecap="round" />
      <text :x="gaugeGeometry.w / 2" :y="gaugeGeometry.cy - 6" text-anchor="middle" :fill="textColor" font-size="30" font-weight="800">{{ gaugePercent }}</text>
      <text :x="gaugeGeometry.w / 2" :y="gaugeGeometry.cy + 16" text-anchor="middle" fill="#8e8e93" font-size="12">{{ component.content || '综合评分' }}</text>
    </svg>
  </div>

  <!-- 时间线 / 履历轴 -->
  <div v-else-if="component.type === 'timeline'" class="visual-timeline">
    <div v-for="(item, i) in timelineItems" :key="`t-${i}`" class="visual-timeline-item">
      <span class="visual-timeline-dot" :style="{ background: accent }"></span>
      <span v-if="i < timelineItems.length - 1" class="visual-timeline-line" :style="{ background: trackColor }"></span>
      <div class="visual-timeline-body">
        <div class="visual-timeline-time" :style="{ color: accent }">{{ item.time }}</div>
        <div class="visual-timeline-title" :style="{ color: textColor }">{{ item.title }}</div>
        <div v-if="item.desc" class="visual-timeline-desc">{{ item.desc }}</div>
      </div>
    </div>
  </div>

  <!-- 词云 / 标签云 -->
  <div v-else-if="component.type === 'wordcloud'" class="visual-wordcloud">
    <span
      v-for="(word, i) in wordItems"
      :key="`w-${i}`"
      class="visual-word"
      :style="wordStyle(word, i)"
    >{{ word.text }}</span>
  </div>

  <!-- 柱状能力图 -->
  <div v-else-if="component.type === 'barchart'" class="visual-barchart">
    <div v-for="(bar, i) in barItems" :key="`b-${i}`" class="visual-bar-row">
      <span class="visual-bar-label" :style="{ color: textColor }">{{ bar.label }}</span>
      <div class="visual-bar-track" :style="{ background: trackColor }">
        <div class="visual-bar-fill" :style="{ width: `${Math.max(0, Math.min(100, bar.value))}%`, background: accent }"></div>
      </div>
      <span class="visual-bar-value" :style="{ color: accent }">{{ bar.value }}</span>
    </div>
  </div>

  <!-- 数据统计卡 -->
  <div v-else-if="component.type === 'statcard'" class="visual-statcard">
    <div v-for="(stat, i) in statItems" :key="`s-${i}`" class="visual-stat">
      <strong class="visual-stat-value" :style="{ color: accent }">{{ stat.value }}</strong>
      <span class="visual-stat-label" :style="{ color: textColor }">{{ stat.label }}</span>
    </div>
  </div>
</template>

<style scoped>
.visual-svg {
  width: 100%;
  height: 100%;
  display: block;
}

.visual-ring {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.visual-ring-svg {
  width: 100%;
  height: calc(100% - 22px);
  min-height: 0;
}
.visual-ring-label {
  font-size: 13px;
  font-weight: 500;
}

.visual-gauge {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.visual-gauge-svg {
  width: 100%;
  height: 100%;
}

.visual-timeline {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 2px 0;
}
.visual-timeline-item {
  position: relative;
  padding-left: 24px;
}
.visual-timeline-dot {
  position: absolute;
  left: 3px;
  top: 4px;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.12);
}
.visual-timeline-line {
  position: absolute;
  left: 8px;
  top: 16px;
  bottom: -16px;
  width: 2px;
}
.visual-timeline-time {
  font-size: 12px;
  font-weight: 600;
}
.visual-timeline-title {
  font-size: 14px;
  font-weight: 600;
  margin-top: 2px;
}
.visual-timeline-desc {
  font-size: 12px;
  color: #6e6e73;
  margin-top: 2px;
  line-height: 1.6;
}

.visual-wordcloud {
  width: 100%;
  height: 100%;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 6px 12px;
  padding: 8px;
}
.visual-word {
  line-height: 1.2;
  white-space: nowrap;
}

.visual-barchart {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}
.visual-bar-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.visual-bar-label {
  width: 72px;
  font-size: 13px;
  font-weight: 500;
  flex-shrink: 0;
  text-align: right;
}
.visual-bar-track {
  flex: 1;
  height: 12px;
  border-radius: 999px;
  overflow: hidden;
}
.visual-bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease;
}
.visual-bar-value {
  width: 32px;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.visual-statcard {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-around;
  gap: 8px;
}
.visual-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.visual-stat-value {
  font-size: 30px;
  font-weight: 800;
  line-height: 1;
}
.visual-stat-label {
  font-size: 12px;
  font-weight: 500;
}
</style>
