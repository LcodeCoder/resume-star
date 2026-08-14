<!--
  简历拖拉拽核心画布组件
  页面位置：简历编辑器主页面
  功能：A4 白纸画布，支持文本、分割线、头像、联系方式等组件点选、拖拽移动、拉角缩放、双击行内编辑文字，
        并接收左侧组件库拖入新增组件；高级组件展示会员标记（不拦截使用）
  交互说明：参考 Canva 编辑器——画布上不常驻组件名称，仅选中时浮出标签芯片
-->
<script setup>
import { computed, nextTick, ref, watch } from 'vue'
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
  applyMediaCrop,
  buildMediaCropStyle,
  canCropMedia,
  getMediaCrop,
  getMediaDrawSize,
  isLetterAvatar,
  isMediaComponent,
  isTextComponent,
  isVisualComponent,
  resetMediaCrop
} from '../../utils/componentStyle'
import ResumeVisual from './ResumeVisual.vue'
import MediaPlaceholder from './MediaPlaceholder.vue'

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
  },
  editable: {
    type: Boolean,
    default: true
  },
  showGrid: {
    type: Boolean,
    default: false
  },
  showRuler: {
    type: Boolean,
    default: false
  },
  snapEnabled: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['select', 'multi-select', 'select-page', 'change', 'add', 'edit-visual', 'require-login'])

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
 * 构建吸附目标线：画布(左/内边距/中线/右)与其它组件(左/中/右)的纵向线，
 * 以及每页(顶/内边距/中线/底)与其它组件(上/中/下)的横向线。
 * excludeIds 内的组件不作为参考目标（拖动自身或整组时排除被拖动的组件）。
 */
const buildSnapTargets = (excludeIds) => {
  const vTargets = [0, 40, PAGE_WIDTH / 2, PAGE_WIDTH - 40, PAGE_WIDTH]
  const hTargets = [0]
  for (let p = 1; p <= pageCount.value; p++) {
    const top = (p - 1) * PAGE_HEIGHT
    hTargets.push(top, top + 40, top + PAGE_HEIGHT / 2, top + PAGE_HEIGHT - 40, top + PAGE_HEIGHT)
  }
  for (const c of visibleComponents.value) {
    if (!c || excludeIds.has(c.id)) continue
    const cx = c.x || 0
    const cw = c.width || 300
    const cy = c.y || 0
    const ch = c.height || 60
    vTargets.push(cx, cx + cw / 2, cx + cw)
    hTargets.push(cy, cy + ch / 2, cy + ch)
  }
  return { vTargets, hTargets }
}

/** 在候选目标线中找到与三条对齐边(左/中/右 或 上/中/下)最近且在阈值内的吸附点 */
const nearestSnap = (targets, edges) => {
  let best = null
  for (const [edge, val] of edges) {
    for (const t of targets) {
      const d = Math.abs(val - t)
      if (d <= SNAP_THRESHOLD && (!best || d < best.d)) best = { d, t, edge }
    }
  }
  return best
}

/**
 * 计算拖动组件的吸附位置并记录参考线：
 * 左/中/右 边对齐到画布(左/中线/右)与其它组件的左/中/右；上/中/下 同理。
 */
const applySnap = (component, x, y, w, h) => {
  if (!props.snapEnabled) {
    guides.value = { x: null, y: null }
    return { x, y }
  }
  const { vTargets, hTargets } = buildSnapTargets(new Set([component.id]))
  let gx = null
  let gy = null
  const bestV = nearestSnap(vTargets, [['left', x], ['center', x + w / 2], ['right', x + w]])
  if (bestV) {
    x = bestV.edge === 'left' ? bestV.t : bestV.edge === 'center' ? bestV.t - w / 2 : bestV.t - w
    gx = bestV.t
  }
  const bestH = nearestSnap(hTargets, [['top', y], ['middle', y + h / 2], ['bottom', y + h]])
  if (bestH) {
    y = bestH.edge === 'top' ? bestH.t : bestH.edge === 'middle' ? bestH.t - h / 2 : bestH.t - h
    gy = bestH.t
  }
  guides.value = { x: gx, y: gy }
  return { x, y }
}

/**
 * 整组拖动吸附：把整组的外接矩形边(左/中/右、上/中/下)对齐到参考线，
 * 返回需要在原位移基础上追加的微调量 (dx, dy)，并记录参考线。
 * 被选中的组件互相之间不作为吸附目标。
 */
const applyGroupSnap = (origins, dx, dy) => {
  if (!props.snapEnabled) {
    guides.value = { x: null, y: null }
    return { dx, dy }
  }
  const excludeIds = new Set(origins.map((item) => item.component.id))
  const { vTargets, hTargets } = buildSnapTargets(excludeIds)
  const left = Math.min(...origins.map((item) => item.x)) + dx
  const right = Math.max(...origins.map((item) => item.x + item.width)) + dx
  const top = Math.min(...origins.map((item) => item.y)) + dy
  const bottom = Math.max(...origins.map((item) => item.y + item.height)) + dy
  let gx = null
  let gy = null
  const bestV = nearestSnap(vTargets, [['left', left], ['center', (left + right) / 2], ['right', right]])
  if (bestV) {
    dx += bestV.edge === 'left' ? bestV.t - left : bestV.edge === 'center' ? bestV.t - (left + right) / 2 : bestV.t - right
    gx = bestV.t
  }
  const bestH = nearestSnap(hTargets, [['top', top], ['middle', (top + bottom) / 2], ['bottom', bottom]])
  if (bestH) {
    dy += bestH.edge === 'top' ? bestH.t - top : bestH.edge === 'middle' ? bestH.t - (top + bottom) / 2 : bestH.t - bottom
    gy = bestH.t
  }
  guides.value = { x: gx, y: gy }
  return { dx, dy }
}

const selectedIdSet = computed(() => new Set(props.selectedIds || []))
const hasMultiSelection = computed(() => (props.selectedIds || []).length > 1)
const visibleComponents = computed(() => props.components.filter((item) => !item.hidden))
const requireEditable = () => {
  if (props.editable) return true
  emit('require-login')
  return false
}
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

/** 框选拖动中：当前虚线框命中的组件，边拉边高亮 */
const marqueeHitSet = computed(() => {
  if (!marquee.value) return new Set()
  const rect = normalizeRect(marquee.value.start, marquee.value.current)
  if (rect.width < 4 && rect.height < 4) return new Set()
  return new Set(
    visibleComponents.value
      .filter((component) => intersects(rect, component))
      .map((component) => component.id)
  )
})

/** 右键按住拖动：在画布上拉出选区并框选多个组件 */
const startMarquee = (event) => {
  if (!pageRef.value) return
  if (!requireEditable()) return
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
  // 按住照片时禁止浏览器原生拖图（半透明分身 / 松手打开图片页）
  if (isMediaComponent(component) || event.target?.closest?.('img')) event.preventDefault()
  emit('select', component.id)
  if (!requireEditable() || component.locked || editingId.value === component.id) return
  if (hasMultiSelection.value && selectedIdSet.value.has(component.id)) {
    const origins = visibleComponents.value
      .filter((item) => selectedIdSet.value.has(item.id) && !item.locked)
      .map((item) => ({
        component: item,
        x: item.x || 0,
        y: item.y || 0,
        width: item.width || 300,
        height: item.height || 60
      }))
    if (!origins.length) return
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
  if (!requireEditable() || component.locked) return
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
    const rawDx = (event.clientX - dragState.startX) / props.zoom
    const rawDy = (event.clientY - dragState.startY) / props.zoom
    const { dx, dy } = applyGroupSnap(dragState.origins, rawDx, rawDy)
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
  if (dragState.mode === 'media-pan') {
    const component = dragState.component
    const dx = (event.clientX - dragState.startX) / props.zoom
    const dy = (event.clientY - dragState.startY) / props.zoom
    applyMediaCrop(component, {
      imageOffsetX: dragState.originX + dx,
      imageOffsetY: dragState.originY + dy
    })
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
    if (canCropMedia(component)) applyMediaCrop(component)
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
  mediaPanning.value = false
  marquee.value = null
  guides.value = { x: null, y: null } // 结束拖动清除参考线
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
}

/**
 * 双击进入行内文字编辑；头像/图片/二维码触发上传；其他结构组件忽略
 */
const startEdit = async (component) => {
  emit('select', component.id)
  if (!requireEditable() || component.locked) return
  if (['avatar', 'image', 'qrcode'].includes(component.type)) {
    triggerUpload(component)
    return
  }
  // 会员高级可视化组件：双击打开数据编辑抽屉（由父级 EditorView 承载）
  if (isVisualComponent(component)) {
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
const mediaPanning = ref(false)
/** 默认拖的是组件本身；点「调整照片」后才在框内拖移照片 */
const mediaPanMode = ref(false)

watch(() => props.selectedId, () => {
  mediaPanMode.value = false
  mediaPanning.value = false
})

const toggleMediaPanMode = () => {
  mediaPanMode.value = !mediaPanMode.value
  mediaPanning.value = false
}

/**
 * 触发图像上传：选中组件并打开文件选择器
 */
const triggerUpload = (component) => {
  emit('select', component.id)
  uploadTarget.value = component
  nextTick(() => uploadInputRef.value?.click())
}

const mediaEmptyLabel = (component) => {
  if ((component.width || 0) < 72 || (component.height || 0) < 72) return ''
  if (component.type === 'qrcode') return '上传二维码'
  if (component.type === 'avatar') return '上传照片'
  return '上传图片'
}
const isCompactMedia = (component) => (component.width || 0) < 72 || (component.height || 0) < 72
const showMediaUpload = (component) =>
  props.editable && !component.locked && component.id === props.selectedId
  && Math.min(component.width || 0, component.height || 0) >= 52

const mediaActionLabel = (component) => {
  if (isCompactMedia(component)) return component.src ? '更换' : '上传'
  if (component.type === 'qrcode') return component.src ? '更换二维码' : '上传二维码'
  if (component.type === 'avatar') return component.src ? '更换照片' : '上传照片'
  return component.src ? '更换图片' : '上传图片'
}

/** 仅「调整照片」模式下拦截指针，避免挡住组件拖移 */
const startMediaPan = (event, component) => {
  if (event.button !== 0) return
  if (!mediaPanMode.value) return
  if (component.id !== props.selectedId || !canCropMedia(component)) return
  if (!requireEditable() || component.locked) return
  event.stopPropagation()
  event.preventDefault()
  mediaPanning.value = true
  dragState = {
    mode: 'media-pan',
    component,
    startX: event.clientX,
    startY: event.clientY,
    originX: component.style?.imageOffsetX || 0,
    originY: component.style?.imageOffsetY || 0
  }
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
}

/** 选中后滚轮缩放框内照片，1x=完整落入框内，最大 4x，并按比例收紧偏移避免露边 */
const onMediaWheel = (event, component) => {
  if (component.id !== props.selectedId || !canCropMedia(component)) return
  if (!requireEditable() || component.locked) return
  event.preventDefault()
  event.stopPropagation()
  const prev = getMediaCrop(component)
  const nextScale = Math.round(Math.min(4, Math.max(1, prev.scale - event.deltaY * 0.0018)) * 100) / 100
  component.style = component.style || {}
  component.style.imageScale = nextScale
  const next = getMediaDrawSize(component)
  applyMediaCrop(component, {
    imageScale: nextScale,
    imageOffsetX: prev.maxX ? prev.x * (next.maxX / prev.maxX) : 0,
    imageOffsetY: prev.maxY ? prev.y * (next.maxY / prev.maxY) : 0
  })
  emit('change')
}

const onMediaLoad = (event, component) => {
  const img = event.target
  if (!img?.naturalWidth || !component) return
  if (!component.style) component.style = {}
  if (component.style.imageNaturalW === img.naturalWidth && component.style.imageNaturalH === img.naturalHeight) return
  component.style.imageNaturalW = img.naturalWidth
  component.style.imageNaturalH = img.naturalHeight
  applyMediaCrop(component)
  emit('change')
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
    resetMediaCrop(target)
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
 * 双击页面内空白处：按点击落点的纵向位置判断处于第几页，选中「该页」（而非整篇）
 */
const selectPage = (e) => {
  if (!pageRef.value) { emit('select-page', 1); return }
  if (!props.editable) emit('require-login')
  const rect = pageRef.value.getBoundingClientRect()
  const y = (e.clientY - rect.top) / props.zoom
  const idx = Math.min(pageCount.value, Math.max(1, Math.floor(y / PAGE_HEIGHT) + 1))
  emit('select-page', idx)
}

/**
 * 页面空白处按下指针：仅右键启动框选；左键单击不再选中整页，改为双击触发（见 @dblclick）
 */
const onPagePointerDown = (e) => {
  if (e.button === 2) startMarquee(e)
}

/** 组件归属页（1 基）：按组件顶部 y 落在哪一页区间 */
const pageIndexOf = (c) => Math.floor((c.y || 0) / PAGE_HEIGHT) + 1

/**
 * 接收左侧组件库拖入：换算落点画布坐标后通知父级新增组件
 */
const onDrop = (event) => {
  if (!requireEditable()) return
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
  if (!requireEditable()) return
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
        @dragstart.prevent
        @contextmenu.prevent
        @pointerdown.self="onPagePointerDown"
        @click.self="clearSelect"
        @dblclick.self="selectPage"
      >
        <!-- 整页选中态：在被选中的那一页区间画高亮带（不拦截点击） -->
        <div
          v-if="props.selectedPage > 0"
          class="page-select-band"
          :style="{ top: `${(props.selectedPage - 1) * PAGE_HEIGHT}px`, height: `${PAGE_HEIGHT}px` }"
        ></div>
        <!-- 分页参考线：每满一页 A4 高度画一条虚线，并标注页码，便于排版换页 -->
        <div v-if="props.showGrid" class="page-grid-overlay"></div>
        <div v-if="props.showRuler" class="ruler-overlay ruler-top"></div>
        <div v-if="props.showRuler" class="ruler-overlay ruler-left"></div>
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
          <p class="empty-title">✨ 从左侧或上方选择一种开始方式</p>
        </div>

        <div
          v-for="component in visibleComponents"
          :key="component.id"
          class="resume-block"
          :class="[
            { selected: component.id === props.selectedId && !hasMultiSelection && !marquee, 'multi-selected': (hasMultiSelection && selectedIdSet.has(component.id)) || marqueeHitSet.has(component.id), editing: component.id === editingId, structural: !isTextComponent(component), locked: component.locked, hidden: component.hidden,
              'on-selected-page': props.selectedPage > 0 && pageIndexOf(component) === props.selectedPage,
              'is-media': isMediaComponent(component),
              'is-cropping': canCropMedia(component) && component.id === props.selectedId && mediaPanMode,
              'is-panning': mediaPanning && component.id === props.selectedId },
            `type-${component.type}`
          ]"
          :style="buildBlockStyle(component)"
          @pointerdown="startMove($event, component)"
          @dblclick="startEdit(component)"
        >
          <!-- 选中时浮出的标签芯片：展示组件名称与会员标记 -->
          <div v-if="component.id === props.selectedId" class="block-chip">
            {{ component.label }}
            <span v-if="component.groupId" class="vip-badge">组合</span>
            <span v-if="component.locked" class="vip-badge">锁定</span>
            <span v-if="component.vipOnly" class="vip-badge">会员</span>
          </div>

          <!-- 分割线组件：用于区隔简历章节 -->
          <div v-if="component.type === 'divider'" class="resume-divider" :style="buildDividerStyle(component)"></div>

          <!-- 头像：照片 / 字母色块 / 空态剪影。选中后底部提供上传按钮 -->
          <div
            v-else-if="component.type === 'avatar'"
            class="resume-avatar media-slot"
            :class="{ 'has-src': !!component.src, 'is-empty': !component.src && !isLetterAvatar(component) }"
            :style="buildAvatarStyle(component)"
            @wheel.prevent="onMediaWheel($event, component)"
          >
            <img
              v-if="component.src"
              :key="component.src"
              :src="component.src"
              :alt="component.content || '简历头像'"
              :style="buildMediaCropStyle(component)"
              draggable="false"
              @dragstart.prevent
              @load="onMediaLoad($event, component)"
              @pointerdown="startMediaPan($event, component)"
            />
            <span v-else-if="isLetterAvatar(component)" class="letter-avatar">{{ component.content }}</span>
            <MediaPlaceholder v-else kind="avatar" :label="mediaEmptyLabel(component)" :compact="isCompactMedia(component)" />
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

          <!-- 二维码：空态用定位点图形，有图时 contain 铺满以免裁切 -->
          <div
            v-else-if="component.type === 'qrcode'"
            class="resume-qrcode media-slot"
            :class="{ 'has-src': !!component.src, 'is-empty': !component.src }"
            :style="buildQrcodeStyle(component)"
          >
            <img v-if="component.src" :key="component.src" :src="component.src" :alt="component.content || '二维码'" draggable="false" @dragstart.prevent />
            <MediaPlaceholder v-else kind="qrcode" :label="mediaEmptyLabel(component)" :compact="isCompactMedia(component)" />
          </div>

          <!-- 图片：空态虚线框 + 图标；选中可直接点按钮上传 -->
          <div
            v-else-if="component.type === 'image'"
            class="resume-image media-slot"
            :class="{ 'has-src': !!component.src, 'is-empty': !component.src }"
            :style="buildImageStyle(component)"
            @wheel.prevent="onMediaWheel($event, component)"
          >
            <img
              v-if="component.src"
              :key="component.src"
              :src="component.src"
              :alt="component.content || '图片'"
              :style="buildMediaCropStyle(component)"
              draggable="false"
              @dragstart.prevent
              @load="onMediaLoad($event, component)"
              @pointerdown="startMediaPan($event, component)"
            />
            <MediaPlaceholder v-else kind="image" :label="mediaEmptyLabel(component)" :compact="isCompactMedia(component)" />
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
          <div
            v-if="isMediaComponent(component) && showMediaUpload(component)"
            class="media-action-bar"
            @pointerdown.stop
          >
            <button
              v-if="canCropMedia(component)"
              type="button"
              class="media-upload-btn"
              :class="{ active: mediaPanMode }"
              @click.stop="toggleMediaPanMode"
            >{{ mediaPanMode ? '完成调整' : '调整照片' }}</button>
            <button
              type="button"
              class="media-upload-btn"
              @click.stop="triggerUpload(component)"
            >{{ mediaActionLabel(component) }}</button>
          </div>
          <span
            v-if="canCropMedia(component) && component.id === props.selectedId && !isCompactMedia(component)"
            class="media-crop-hint"
          >{{ mediaPanMode ? '拖动照片取景 · 滚轮缩放' : '拖动移动位置 · 滚轮放大' }}</span>
          <span
            v-if="component.id === props.selectedId && !component.locked && props.editable"
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

/* 选中时浮出的组件名芯片：绝对定位浮层，不占流、不拦截点击，避免选中瞬间撑高块导致画布抖动 */
.block-chip {
  position: absolute;
  left: 0;
  bottom: 100%;
  margin-bottom: 4px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(91, 91, 214, 0.92);
  color: #fff;
  font-size: 11px;
  line-height: 1.5;
  white-space: nowrap;
  pointer-events: none;
  z-index: 6;
}
.block-chip .vip-badge {
  padding: 0 5px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.24);
  color: #fff;
  font-size: 10px;
}

/* 图片类：选中只用 outline，避免 padding/阴影改变内容盒导致照片左右跳 */
.resume-block.is-media {
  padding: 0;
  border: none;
}
.resume-block.is-media.selected,
.resume-block.is-media.multi-selected {
  border: none;
  box-shadow: none;
}
.resume-block.is-media.selected .media-slot,
.resume-block.is-media.multi-selected .media-slot {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.media-slot {
  position: relative;
  box-sizing: border-box;
}
.media-slot img {
  display: block;
  pointer-events: auto;
  user-select: none;
  -webkit-user-drag: none;
}
.resume-qrcode.media-slot img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #fff;
  transform: none;
}
.letter-avatar {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  font-weight: 700;
  letter-spacing: .04em;
}
.resume-block.is-cropping .media-slot.has-src {
  cursor: grab;
}
.resume-block.is-panning .media-slot.has-src {
  cursor: grabbing;
}
.media-action-bar {
  position: absolute;
  z-index: 7;
  left: 50%;
  bottom: 8px;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
}
.media-upload-btn {
  min-height: 26px;
  padding: 0 10px;
  border: 0;
  border-radius: 999px;
  background: #fff;
  color: #1d1d1f;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .02em;
  line-height: 1;
  white-space: nowrap;
  box-shadow: 0 4px 12px rgba(17, 24, 39, 0.18);
  cursor: pointer;
}
.media-upload-btn:hover,
.media-upload-btn.active {
  background: var(--accent);
  color: var(--on-accent);
}
.media-crop-hint {
  position: absolute;
  z-index: 7;
  left: 50%;
  top: 8px;
  transform: translateX(-50%);
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.62);
  color: #fff;
  font-size: 10px;
  font-weight: 650;
  line-height: 1.4;
  white-space: nowrap;
  pointer-events: none;
}
.resume-block.is-media:not(.selected):hover .media-slot.is-empty {
  filter: brightness(0.97);
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

/* 网格叠层：以 0.5cm(≈18.9px) 为单元铺满整页，辅助对齐排版；不拦截指针 */
.page-grid-overlay {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
  background-image:
    linear-gradient(to right, rgba(91, 91, 214, 0.09) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(91, 91, 214, 0.09) 1px, transparent 1px);
  background-size: 18.9px 18.9px;
}

/* 标尺叠层：顶部横标尺 + 左侧竖标尺，刻度间距 0.5cm(≈18.9px) */
.ruler-overlay {
  position: absolute;
  z-index: 6;
  pointer-events: none;
  background: rgba(245, 245, 247, 0.92);
}
.ruler-top {
  top: 0;
  left: 0;
  right: 0;
  height: 16px;
  border-bottom: 1px solid #d0d0d5;
  background-image: repeating-linear-gradient(
    to right,
    #b0b0b8 0,
    #b0b0b8 1px,
    transparent 1px,
    transparent 18.9px
  );
}
.ruler-left {
  top: 0;
  left: 0;
  bottom: 0;
  width: 16px;
  border-right: 1px solid #d0d0d5;
  background-image: repeating-linear-gradient(
    to bottom,
    #b0b0b8 0,
    #b0b0b8 1px,
    transparent 1px,
    transparent 18.9px
  );
}

/* 分页线：每满一页 A4 高度画一条虚线并标注页码 */
.page-break {
  position: absolute;
  left: 0;
  right: 0;
  z-index: 2;
  border-top: 1px dashed #c0c4cc;
  pointer-events: none;
}
.page-break-label {
  position: absolute;
  right: 8px;
  top: 4px;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.9);
  color: #a0a4ac;
  font-size: 11px;
  line-height: 1.4;
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
