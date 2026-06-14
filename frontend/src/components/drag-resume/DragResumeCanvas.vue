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

const emit = defineEmits(['select', 'change', 'add', 'edit-visual'])

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
  if (editingId.value === component.id) return
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
  const { component } = dragState
  const dx = (event.clientX - dragState.startX) / props.zoom
  const dy = (event.clientY - dragState.startY) / props.zoom
  if (dragState.mode === 'move') {
    const width = component.width || 300
    component.x = Math.round(Math.min(Math.max(dragState.originX + dx, 0), PAGE_WIDTH - width))
    // 纵向允许跨页：上限在当前内容基础上再留一整页，拖到底部会自动新增页面
    component.y = Math.round(Math.min(Math.max(dragState.originY + dy, 0), maxDragBottom.value - 40))
  } else {
    component.width = Math.round(Math.min(Math.max(dragState.originW + dx, 48), PAGE_WIDTH - (component.x || 0)))
    component.height = Math.round(Math.max(dragState.originH + dy, component.type === 'divider' ? 2 : 32))
  }
}

/**
 * 指针抬起：结束拖拽并通知父级数据已变更（触发自动保存）
 */
const onPointerUp = () => {
  if (dragState) emit('change')
  dragState = null
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
 * 点击画布空白处取消选中
 */
const clearSelect = () => emit('select', '')

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
        @pointerdown.self="clearSelect"
      >
        <!-- 分页参考线：每满一页 A4 高度画一条虚线，并标注页码，便于排版换页 -->
        <div
          v-for="n in (pageCount - 1)"
          :key="`pb-${n}`"
          class="page-break"
          :style="{ top: `${n * 1123}px` }"
        >
          <span class="page-break-label">第 {{ n + 1 }} 页</span>
        </div>

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
            { selected: component.id === props.selectedId, editing: component.id === editingId, structural: !isTextComponent(component) },
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
          <!-- 编辑态字数统计：实时显示当前组件内容字数 -->
          <span v-if="component.id === editingId" class="block-charcount">
            {{ (component.content || '').length }} 字
          </span>
          <div v-else class="resume-block-content" :style="contentStyle(component)">{{ component.content }}</div>

          <!-- 右下角缩放手柄 -->
          <span
            v-if="component.id === props.selectedId"
            class="resize-handle"
            @pointerdown.stop.prevent="startResize($event, component)"
          ></span>
        </div>

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
</style>
