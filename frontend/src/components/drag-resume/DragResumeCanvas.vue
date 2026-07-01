<!--
  简历拖拉拽核心画布组件
  页面位置：简历编辑器主页面
  功能：A4 白纸画布，支持文本、分割线、头像、联系方式等组件点选、拖拽移动、拉角缩放、双击行内编辑文字，
        并接收左侧组件库拖入新增组件；高级组件展示会员标记（不拦截使用）
  交互说明：参考 Canva 编辑器——画布上不常驻组件名称，仅选中时浮出标签芯片
-->
<script setup>
import { computed, nextTick, ref } from 'vue'
import {
  buildAvatarStyle,
  buildBlockStyle,
  buildComponentStyle,
  buildDividerStyle,
  buildImageStyle,
  buildProgressStyle,
  buildQrcodeStyle,
  buildRatingStyle,
  buildTagStyle,
  getContactIcon,
  isTextComponent,
  isVisualComponent
} from '../../utils/componentStyle'
import ResumeVisual from './ResumeVisual.vue'

const props = defineProps({
  /** 画布组件数据列表（直接修改其中对象的坐标/内容，变更通过 change 事件通知父级） */
  components: {
    type: Array,
    default: () => []
  },
  /** 当前选中组件 ID */
  selectedId: {
    type: String,
    default: ''
  },
  /** 当前多选组件 ID 列表 */
  selectedIds: {
    type: Array,
    default: () => []
  },
  /** 是否「整页选中」态（点击页面空白处触发，高亮整页、可一键清空） */
  selectedPage: {
    type: Number,
    default: 0
  },
  /** 画布缩放比例 */
  zoom: {
    type: Number,
    default: 1
  },
  /** 简历纸张级样式：背景色等整页设置 */
  pageStyle: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['select', 'multi-select', 'select-page', 'change', 'add', 'edit-visual'])

/** A4 纸张尺寸（96dpi 像素） */
const PAGE_WIDTH = 794
const PAGE_HEIGHT = 1123

/**
 * 根据组件实际占位自动计算需要的 A4 页数：
 * 取所有组件底部最大值，向上取整成整页数，至少 1 页；内容超过一页即自动向下增加页面
 */
const pageCount = computed(() => {
  const maxBottom = props.components.reduce(
    (max, c) => Math.max(max, (c.y || 0) + (c.height || 0)), 0
  )
  return Math.max(1, Math.ceil((maxBottom + 24) / PAGE_HEIGHT))
})
/** 画布总高度（含所有页面） */
const pageHeight = computed(() => pageCount.value * PAGE_HEIGHT)
/** 拖拽时允许的纵向上限：在当前内容基础上再预留一整页，便于把组件拖到新页 */
const maxDragBottom = computed(() => (pageCount.value + 1) * PAGE_HEIGHT)

const pageRef = ref(null)
/** 当前处于行内编辑状态的组件 ID */
const editingId = ref('')
/** 拖拽 / 缩放过程中的临时状态 */
let dragState = null
/** 右键框选区域（画布坐标） */
const marquee = ref(null)

/** 拖动对齐参考线（页面坐标，null 表示不显示）；吸附阈值 */
const guides = ref({ x: null, y: null })
const SNAP_THRESHOLD = 6

/**
 * 计算拖动组件的吸附位置并记录参考线：
 * 左/中/右 边对齐到画布(左/中线/右)与其它组件的左/中/右；上/中/下 同理。
 */
const applySnap = (component, x, y, w, h) => {
  const vTargets = [0, PAGE_WIDTH / 2, PAGE_WIDTH]
  const hTargets = [0]
  for (const c of props.components) {
    if (!c || c.id === component.id) continue
    const cx = c.x || 0
    const cw = c.width || 300
    const cy = c.y || 0
    const ch = c.height || 60
    vTargets.push(cx, cx + cw / 2, cx + cw)
    hTargets.push(cy, cy + ch / 2, cy + ch)
  }
  let gx = null
  let bestV = null
  for (const [edge, val] of [['left', x], ['center', x + w / 2], ['right', x + w]]) {
    for (const t of vTargets) {
      const d = Math.abs(val - t)
      if (d <= SNAP_THRESHOLD && (!bestV || d < bestV.d)) bestV = { d, t, edge }
    }
  }
  if (bestV) {
    x = bestV.edge === 'left' ? bestV.t : bestV.edge === 'center' ? bestV.t - w / 2 : bestV.t - w
    gx = bestV.t
  }
  let gy = null
  let bestH = null
  for (const [edge, val] of [['top', y], ['middle', y + h / 2], ['bottom', y + h]]) {
    for (const t of hTargets) {
      const d = Math.abs(val - t)
      if (d <= SNAP_THRESHOLD && (!bestH || d < bestH.d)) bestH = { d, t, edge }
    }
  }
  if (bestH) {
    y = bestH.edge === 'top' ? bestH.t : bestH.edge === 'middle' ? bestH.t - h / 2 : bestH.t - h
    gy = bestH.t
  }
  guides.value = { x: gx, y: gy }
  return { x, y }
}

const selectedIdSet = computed(() => new Set(props.selectedIds || []))
const hasMultiSelection = computed(() => (props.selectedIds || []).length > 1)

/** 将鼠标坐标换算成未缩放的画布坐标 */
const pointToPage = (event) => {
  const rect = pageRef.value.getBoundingClientRect()
  return {
    x: (event.clientX - rect.left) / props.zoom,
    y: (event.clientY - rect.top) / props.zoom
  }
}

const normalizeRect = (a, b) => ({
  left: Math.min(a.x, b.x),
  top: Math.min(a.y, b.y),
  width: Math.abs(a.x - b.x),
  height: Math.abs(a.y - b.y)
})

const intersects = (rect, component) => {
  const x = component.x || 0
  const y = component.y || 0
  const w = component.width || 300
  const h = component.height || 60
  return rect.left < x + w
    && rect.left + rect.width > x
    && rect.top < y + h
    && rect.top + rect.height > y
}

const marqueeStyle = computed(() => {
  if (!marquee.value) return null
  const rect = normalizeRect(marquee.value.start, marquee.value.current)
  return {
    left: `${rect.left}px`,
    top: `${rect.top}px`,
    width: `${rect.width}px`,
    height: `${rect.height}px`
  }
})

/** 右键按住拖动：在画布上拉出选区并框选多个组件 */
const startMarquee = (event) => {
  if (!pageRef.value) return
  event.preventDefault()
  const point = pointToPage(event)
  marquee.value = { start: point, current: point }
  dragState = {
    mode: 'marquee',
    pointerId: event.pointerId
  }
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
}

/**
 * 根据组件样式配置生成文字内联样式（与模板缩略图共用工具方法）
 * @param component 画布组件对象
 */
const contentStyle = (component) => buildComponentStyle(component)

/**
 * 生成简历纸张样式，页面背景色走简历级配置，便于统一换肤与打印
 */
const resumePageStyle = () => ({
  transform: `scale(${props.zoom})`,
  height: `${pageHeight.value}px`,
  background: props.pageStyle?.background || '#ffffff'
})

/**
 * 按住组件开始拖拽移动
 * 作用：记录起始坐标，监听全局指针事件更新组件位置（除以缩放比例换算画布坐标）
 */
const startMove = (event, component) => {
  if (event.button === 2) {
    startMarquee(event)
    return
  }
  if (editingId.value === component.id) return
  if (hasMultiSelection.value && selectedIdSet.value.has(component.id)) {
    const origins = props.components
      .filter((item) => selectedIdSet.value.has(item.id))
      .map((item) => ({
        component: item,
        x: item.x || 0,
        y: item.y || 0,
        width: item.width || 300,
        height: item.height || 60
      }))
    dragState = {
      mode: 'group-move',
      origins,
      startX: event.clientX,
      startY: event.clientY
    }
    window.addEventListener('pointermove', onPointerMove)
    window.addEventListener('pointerup', onPointerUp)
    return
  }
  emit('select', component.id)
  dragState = {
    mode: 'move',
    component,
    startX: event.clientX,
    startY: event.clientY,
    originX: component.x || 0,
    originY: component.y || 0
  }
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
}

/**
 * 按住右下角手柄开始缩放组件
 */
const startResize = (event, component) => {
  emit('select', component.id)
  dragState = {
    mode: 'resize',
    component,
    startX: event.clientX,
    startY: event.clientY,
    originW: component.width || 300,
    originH: component.height || 60
  }
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
}

/**
 * 指针移动：根据模式更新位置或尺寸，并约束在画布范围内
 */
const onPointerMove = (event) => {
  if (!dragState) return
  if (dragState.mode === 'marquee') {
    marquee.value.current = pointToPage(event)
    return
  }
  if (dragState.mode === 'group-move') {
    const dx = (event.clientX - dragState.startX) / props.zoom
    const dy = (event.clientY - dragState.startY) / props.zoom
    const minDx = Math.max(...dragState.origins.map((item) => -item.x))
    const maxDx = Math.min(...dragState.origins.map((item) => PAGE_WIDTH - item.width - item.x))
    const minDy = Math.max(...dragState.origins.map((item) => -item.y))
    const maxDy = Math.min(...dragState.origins.map((item) => maxDragBottom.value - 40 - item.y))
    const nextDx = Math.round(Math.min(Math.max(dx, minDx), maxDx))
    const nextDy = Math.round(Math.min(Math.max(dy, minDy), maxDy))
    for (const item of dragState.origins) {
      item.component.x = item.x + nextDx
      item.component.y = item.y + nextDy
    }
    return
  }
  const { component } = dragState
  const dx = (event.clientX - dragState.startX) / props.zoom
  const dy = (event.clientY - dragState.startY) / props.zoom
  if (dragState.mode === 'move') {
    const width = component.width || 300
    const height = component.height || 60
    const rawX = Math.min(Math.max(dragState.originX + dx, 0), PAGE_WIDTH - width)
    const rawY = Math.max(dragState.originY + dy, 0)
    const snapped = applySnap(component, rawX, rawY, width, height)
    component.x = Math.round(Math.min(Math.max(snapped.x, 0), PAGE_WIDTH - width))
    // 纵向允许跨页：上限在当前内容基础上再留一整页，拖到底部会自动新增页面
    component.y = Math.round(Math.min(Math.max(snapped.y, 0), maxDragBottom.value - 40))
  } else {
    component.width = Math.round(Math.min(Math.max(dragState.originW + dx, 48), PAGE_WIDTH - (component.x || 0)))
    component.height = Math.round(Math.max(dragState.originH + dy, component.type === 'divider' ? 2 : 32))
  }
}

/**
 * 指针抬起：结束拖拽并通知父级数据已变更（触发自动保存）
 */
const onPointerUp = () => {
  if (dragState?.mode === 'marquee') {
    const rect = normalizeRect(marquee.value.start, marquee.value.current)
    const ids = rect.width < 4 && rect.height < 4
      ? []
      : props.components.filter((component) => intersects(rect, component)).map((component) => component.id)
    emit('multi-select', ids)
  } else if (dragState) {
    emit('change')
  }
  dragState = null
  marquee.value = null
  guides.value = { x: null, y: null } // 结束拖动清除参考线
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
}

/**
 * 双击进入行内文字编辑；头像/图片/二维码触发上传；其他结构组件忽略
 */
const startEdit = async (component) => {
  if (['avatar', 'image', 'qrcode'].includes(component.type)) {
    triggerUpload(component)
    return
  }
  // 会员高级可视化组件：双击打开数据编辑抽屉（由父级 EditorView 承载）
  if (isVisualComponent(component)) {
    emit('select', component.id)
    emit('edit-visual', component.id)
    return
  }
  if (!isTextComponent(component)) return
  editingId.value = component.id
  emit('select', component.id)
  await nextTick()
  const textarea = pageRef.value?.querySelector('.block-editor')
  textarea?.focus()
  // 进入编辑即按当前内容撑高，避免初始出现滚动条
  if (textarea) autoGrow({ target: textarea }, component)
}

/**
 * 文本框随内容自动撑高：清掉滚动条，同时把组件高度同步到实际内容高度，
 * 这样退出编辑后画布块高度与可见内容一致，所见即所得。
 */
const autoGrow = (event, component) => {
  const el = event.target
  if (!el) return
  el.style.height = 'auto'
  const next = el.scrollHeight
  el.style.height = `${next}px`
  // 同步组件高度（最小 24px），让块容器跟随内容增高
  component.height = Math.max(24, next)
}

/** 当前等待文件的组件（隐藏 input change 时使用） */
const uploadTarget = ref(null)
const uploadInputRef = ref(null)

/**
 * 触发图像上传：选中组件并打开文件选择器
 */
const triggerUpload = (component) => {
  emit('select', component.id)
  uploadTarget.value = component
  // 等待 DOM 更新后再触发 click，确保 input 存在
  nextTick(() => uploadInputRef.value?.click())
}

/**
 * 图像文件选择回调：转 dataURL 写入 component.src
 */
const onUploadChange = (event) => {
  const file = event.target.files?.[0]
  const target = uploadTarget.value
  if (!file || !target) {
    event.target.value = ''
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    target.src = reader.result
    emit('change')
  }
  reader.readAsDataURL(file)
  event.target.value = ''
  uploadTarget.value = null
}

/**
 * 结束行内编辑并通知变更
 */
const finishEdit = () => {
  editingId.value = ''
  emit('change')
}

/**
 * 点击画布最外层空白处（页面之外）：彻底取消选中
 */
const clearSelect = () => emit('select', '')

/**
 * 点击页面内空白处：按点击落点的纵向位置判断处于第几页，选中「该页」（而非整篇）
 */
const selectPage = (e) => {
  if (e.button === 2) {
    startMarquee(e)
    return
  }
  if (!pageRef.value) { emit('select-page', 1); return }
  const rect = pageRef.value.getBoundingClientRect()
  const y = (e.clientY - rect.top) / props.zoom
  const idx = Math.min(pageCount.value, Math.max(1, Math.floor(y / PAGE_HEIGHT) + 1))
  emit('select-page', idx)
}

/** 组件归属页（1 基）：按组件顶部 y 落在哪一页区间 */
const pageIndexOf = (c) => Math.floor((c.y || 0) / PAGE_HEIGHT) + 1

/**
 * 接收左侧组件库拖入：换算落点画布坐标后通知父级新增组件
 */
const onDrop = (event) => {
  const raw = event.dataTransfer.getData('application/json')
  if (!raw) return
  const payload = JSON.parse(raw)
  const rect = pageRef.value.getBoundingClientRect()
  const width = payload.width || 300
  const x = (event.clientX - rect.left) / props.zoom - width / 2
  const y = (event.clientY - rect.top) / props.zoom - 20
  emit('add', {
    ...payload,
    x: Math.round(Math.min(Math.max(x, 0), PAGE_WIDTH - width)),
    y: Math.round(Math.min(Math.max(y, 0), maxDragBottom.value - 60))
  })
}

/**
 * 新增空白页：在当前内容底部添加一个占位文本组件，触发自动增页
 */
const addNewPage = () => {
  const newPageY = pageCount.value * PAGE_HEIGHT + 50
  emit('add', {
    type: 'text',
    label: '文本',
    content: '新页面开始，可拖拽或删除此提示文本',
    x: 50,
    y: newPageY,
    width: 694,
    height: 40,
    style: { fontSize: 14, color: '#999999', textAlign: 'center' }
  })
}
</script>

<template>
  <div class="canvas-area" @pointerdown.self="clearSelect">
    <!-- 缩放包裹层：按缩放比例占位，避免滚动区域计算错误 -->
    <div class="page-scaler" :style="{ width: `${794 * props.zoom}px`, height: `${pageHeight * props.zoom}px` }">
      <div
        ref="pageRef"
        class="resume-page"
        :class="{ 'editing-mode': !!editingId }"
        :style="resumePageStyle()"
        @dragover.prevent
        @drop.prevent="onDrop"
        @contextmenu.prevent
        @pointerdown.self="selectPage"
        @click.self="selectPage"
      >
        <!-- 整页选中态：在被选中的那一页区间画高亮带（不拦截点击） -->
        <div
          v-if="props.selectedPage > 0"
          class="page-select-band"
          :style="{ top: `${(props.selectedPage - 1) * PAGE_HEIGHT}px`, height: `${PAGE_HEIGHT}px` }"
        ></div>
        <!-- 分页参考线：每满一页 A4 高度画一条虚线，并标注页码，便于排版换页 -->
        <div
          v-for="n in (pageCount - 1)"
          :key="`pb-${n}`"
          class="page-break"
          :style="{ top: `${n * 1123}px` }"
        >
          <span class="page-break-label">第 {{ n + 1 }} 页</span>
        </div>

        <!-- 拖动对齐参考线：吸附到画布中线/边或其它组件时显示 -->
        <div v-if="guides.x !== null" class="snap-guide snap-guide-v" :style="{ left: `${guides.x}px` }"></div>
        <div v-if="guides.y !== null" class="snap-guide snap-guide-h" :style="{ top: `${guides.y}px` }"></div>

        <!-- 空白简历引导提示 -->
        <div v-if="!props.components || props.components.length === 0" class="empty-canvas-hint">
          <p class="empty-title">✨ 开始创建你的简历</p>
          <p class="empty-desc">从左侧组件库拖拽组件到画布，或点击组件快速添加</p>
          <p class="empty-desc">也可以选择左侧的模板，一键套用专业简历布局</p>
        </div>

        <div
          v-for="component in props.components"
          :key="component.id"
          class="resume-block"
          :class="[
            { selected: component.id === props.selectedId && !hasMultiSelection, 'multi-selected': hasMultiSelection && selectedIdSet.has(component.id), editing: component.id === editingId, structural: !isTextComponent(component),
              'on-selected-page': props.selectedPage > 0 && pageIndexOf(component) === props.selectedPage },
            `type-${component.type}`
          ]"
          :style="buildBlockStyle(component)"
          @pointerdown="startMove($event, component)"
          @dblclick="startEdit(component)"
        >
          <!-- 选中时浮出的标签芯片：展示组件名称与会员标记 -->
          <div v-if="component.id === props.selectedId" class="block-chip">
            {{ component.label }}
            <span v-if="component.vipOnly" class="vip-badge">会员</span>
          </div>

          <!-- 分割线组件：用于区隔简历章节 -->
          <div v-if="component.type === 'divider'" class="resume-divider" :style="buildDividerStyle(component)"></div>

          <!-- 头像组件：有图片时显示图片，没有图片时显示可替换占位头像。双击触发上传 -->
          <div v-else-if="component.type === 'avatar'" class="resume-avatar" :style="buildAvatarStyle(component)">
            <img v-if="component.src" :key="component.src" :src="component.src" :alt="component.content || '简历头像'" />
            <span v-else>{{ component.content || '头像' }}</span>
            <span v-if="component.id === props.selectedId" class="upload-hint">双击上传</span>
          </div>

          <!-- 图标组件：统一 SVG 小图标 + 可编辑文字（文字留空时仅显示图标） -->
          <div v-else-if="component.type === 'contact'" class="resume-contact" :style="contentStyle(component)">
            <svg
              class="contact-icon"
              :viewBox="getContactIcon(component).viewBox"
              aria-hidden="true"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path v-for="path in getContactIcon(component).paths" :key="path" :d="path" />
            </svg>
            <span v-if="component.content">{{ component.content }}</span>
          </div>

          <!-- 色块组件：纯背景色矩形，用于章节色带、留白填充 -->
          <div v-else-if="component.type === 'block'" class="resume-color-block"></div>

          <!-- 进度条组件：标签 + 百分比条 -->
          <div v-else-if="component.type === 'progress'" class="resume-progress">
            <div class="progress-label" :style="buildProgressStyle(component).label">
              <span>{{ component.content }}</span>
              <span>{{ component.style?.percent ?? 60 }}%</span>
            </div>
            <div class="progress-track" :style="buildProgressStyle(component).track">
              <div class="progress-fill" :style="buildProgressStyle(component).fill"></div>
            </div>
          </div>

          <!-- 评分/等级组件：星形或圆点 -->
          <div v-else-if="component.type === 'rating'" class="resume-rating">
            <span :style="buildRatingStyle(component).label">{{ component.content }}</span>
            <span class="rating-items">
              <span
                v-for="i in component.style?.total || 5"
                :key="i"
                class="rating-item"
                :class="{ filled: i <= (component.style?.stars || 0), dot: component.style?.shape === 'dot' }"
                :style="buildRatingStyle(component).item"
              >{{ component.style?.shape === 'dot' ? '●' : '★' }}</span>
            </span>
          </div>

          <!-- 标签组件：胶囊/方角 -->
          <span v-else-if="component.type === 'tag'" class="resume-tag" :style="buildTagStyle(component)">{{ component.content }}</span>

          <!-- 二维码占位（上传后显示图片） -->
          <div v-else-if="component.type === 'qrcode'" :style="component.src ? null : buildQrcodeStyle(component)" class="resume-qrcode">
            <img v-if="component.src" :key="component.src" :src="component.src" :alt="component.content" />
            <span v-else class="qrcode-label">{{ component.content }}</span>
            <span v-if="component.id === props.selectedId" class="upload-hint">双击上传</span>
          </div>

          <!-- 图片占位（无 src 时显示占位） -->
          <div v-else-if="component.type === 'image'" :style="buildImageStyle(component)" class="resume-image">
            <img v-if="component.src" :key="component.src" :src="component.src" :alt="component.content" />
            <span v-else>{{ component.content || '图片占位' }}</span>
            <span v-if="component.id === props.selectedId" class="upload-hint">双击上传</span>
          </div>

          <!-- 会员高级可视化组件：雷达图 / 环形 / 仪表盘 / 时间线 / 词云 / 柱状 / 数据卡 -->
          <ResumeVisual v-else-if="isVisualComponent(component)" :component="component" />

          <!-- 行内编辑态：textarea 替换文本展示，随内容自动撑高（无滚动条） -->
          <textarea
            v-else-if="component.id === editingId"
            v-model="component.content"
            class="block-editor"
            :style="contentStyle(component)"
            @input="autoGrow($event, component)"
            @blur="finishEdit"
            @keydown.esc.prevent="finishEdit"
            @pointerdown.stop
          ></textarea>
          <!-- 文本展示态：仅当未匹配上面任何结构类型且非编辑态时渲染（纯文本组件），不会与图标等组件重复出文字 -->
          <div v-else class="resume-block-content" :style="contentStyle(component)">{{ component.content }}</div>

          <!-- 编辑态字数统计浮层：独立 v-if，不参与上面的类型 v-if 链，避免把展示态从链里挤出去导致内容重复渲染 -->
          <span v-if="component.id === editingId" class="block-charcount">
            {{ (component.content || '').length }} 字
          </span>

          <!-- 右下角缩放手柄 -->
          <span
            v-if="component.id === props.selectedId"
            class="resize-handle"
            @pointerdown.stop.prevent="startResize($event, component)"
          ></span>
        </div>

        <div v-if="marquee" class="marquee-selection" :style="marqueeStyle"></div>

        <!-- 新增页面按钮：显示在最后一页底部 -->
        <button
          class="add-page-btn"
          :style="{ top: `${pageHeight - 60}px` }"
          @click="addNewPage"
        >
          ＋ 新增空白页
        </button>
      </div>
    </div>
    <!-- 图像上传隐藏 input：头像 / 图片 / 二维码组件双击触发 -->
    <input ref="uploadInputRef" type="file" accept="image/*" style="display:none" @change="onUploadChange" />
  </div>
</template>


<style scoped>
/* 右键框选区域 */
.marquee-selection {
  position: absolute;
  z-index: 80;
  border: 1px dashed #2563eb;
  background: rgba(37, 99, 235, 0.12);
  pointer-events: none;
}

/* 行内编辑时的实时字数角标 */
.block-charcount {
  position: absolute;
  right: 2px;
  bottom: -18px;
  font-size: 11px;
  line-height: 1;
  color: #fff;
  background: rgba(91, 91, 214, 0.85);
  padding: 3px 6px;
  border-radius: 6px;
  pointer-events: none;
  white-space: nowrap;
  z-index: 5;
}

/* 焦点高亮：编辑某个文本块时，其余块降透明度以聚焦当前编辑区 */
.resume-page.editing-mode .resume-block:not(.editing) {
  opacity: 0.45;
  transition: opacity 0.2s ease;
}

.resume-page.editing-mode .resume-block.editing {
  transition: opacity 0.2s ease;
}

/* 整页选中态：高亮被选中那一页的区间带 + 该页内组件统一描边 */
.page-select-band {
  position: absolute;
  left: 0;
  width: 100%;
  outline: 2px solid #2563eb;
  outline-offset: -2px;
  background: rgba(37, 99, 235, 0.07);
  pointer-events: none;
  z-index: 4;
}
.resume-block.on-selected-page {
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.5);
}

/* 空白画布引导提示 */
.empty-canvas-hint {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #909399;
  pointer-events: none;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #606266;
  margin: 0 0 12px;
}

.empty-desc {
  font-size: 14px;
  margin: 8px 0;
  line-height: 1.6;
}

/* 新增页面按钮 */
.add-page-btn {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  padding: 10px 24px;
  background: #f5f7fa;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  z-index: 10;
}

.add-page-btn:hover {
  background: #ecf5ff;
  border-color: #5b5bd6;
  color: #5b5bd6;
}

/* 拖动对齐参考线：洋红细线，覆盖整页宽/高，不拦截指针 */
.snap-guide {
  position: absolute;
  z-index: 50;
  pointer-events: none;
  background: #ff2d92;
}

.snap-guide-v {
  top: 0;
  bottom: 0;
  width: 1px;
}

.snap-guide-h {
  left: 0;
  right: 0;
  height: 1px;
}
</style>
