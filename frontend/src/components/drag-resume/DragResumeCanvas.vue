<!--
  简历拖拉拽核心画布组件
  页面位置：简历编辑器主页面
  功能：A4 白纸画布，支持文本、分割线、头像、联系方式等组件点选、拖拽移动、拉角缩放、双击行内编辑文字，
        并接收左侧组件库拖入新增组件；高级组件展示会员标记（不拦截使用）
  交互说明：参考 Canva 编辑器——画布上不常驻组件名称，仅选中时浮出标签芯片
-->
<script setup>
import { nextTick, ref } from 'vue'
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
  isTextComponent
} from '../../utils/componentStyle'

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

const emit = defineEmits(['select', 'change', 'add'])

/** A4 纸张尺寸（96dpi 像素） */
const PAGE_WIDTH = 794
const PAGE_HEIGHT = 1123

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
    component.y = Math.round(Math.min(Math.max(dragState.originY + dy, 0), PAGE_HEIGHT - 40))
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
  if (!isTextComponent(component)) return
  editingId.value = component.id
  emit('select', component.id)
  await nextTick()
  const textarea = pageRef.value?.querySelector('.block-editor')
  textarea?.focus()
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
    y: Math.round(Math.min(Math.max(y, 0), PAGE_HEIGHT - 60))
  })
}
</script>

<template>
  <div class="canvas-area" @pointerdown.self="clearSelect">
    <!-- 缩放包裹层：按缩放比例占位，避免滚动区域计算错误 -->
    <div class="page-scaler" :style="{ width: `${794 * props.zoom}px`, height: `${1123 * props.zoom}px` }">
      <div
        ref="pageRef"
        class="resume-page"
        :style="resumePageStyle()"
        @dragover.prevent
        @drop.prevent="onDrop"
        @pointerdown.self="clearSelect"
      >
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

          <!-- 联系方式组件：统一 SVG 小图标 + 联系信息文本 -->
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
            <span>{{ component.content }}</span>
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

          <!-- 行内编辑态：textarea 替换文本展示 -->
          <textarea
            v-else-if="component.id === editingId"
            v-model="component.content"
            class="block-editor"
            :style="contentStyle(component)"
            @blur="finishEdit"
            @keydown.esc.prevent="finishEdit"
            @pointerdown.stop
          ></textarea>
          <div v-else class="resume-block-content" :style="contentStyle(component)">{{ component.content }}</div>

          <!-- 右下角缩放手柄 -->
          <span
            v-if="component.id === props.selectedId"
            class="resize-handle"
            @pointerdown.stop.prevent="startResize($event, component)"
          ></span>
        </div>
      </div>
    </div>
    <!-- 图像上传隐藏 input：头像 / 图片 / 二维码组件双击触发 -->
    <input ref="uploadInputRef" type="file" accept="image/*" style="display:none" @change="onUploadChange" />
  </div>
</template>
