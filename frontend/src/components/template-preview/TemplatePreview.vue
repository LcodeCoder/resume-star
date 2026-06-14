<!--
  模板缩略图组件
  页面位置：模板库列表、首页热门模板、编辑器左侧模板面板
  功能：按模板组件数据等比缩放渲染一张迷你 A4 简历，所见即所得地展示模板真实样子；
        支持文本、分割线、头像、联系方式图标等组件类型
-->
<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
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
  isVisualComponent
} from '../../utils/componentStyle'
import ResumeVisual from '../drag-resume/ResumeVisual.vue'

const props = defineProps({
  /** 模板组件数据列表（坐标基于 794 x 1123 的 A4 画布） */
  components: {
    type: Array,
    default: () => []
  },
  /** 简历纸张级样式：背景色等整页设置 */
  pageStyle: {
    type: Object,
    default: () => ({})
  },
  /** 缩略图尺寸：紧凑/中等/大图，避免模板卡片预览过大 */
  size: {
    type: String,
    default: 'medium'
  }
})

/** A4 画布基准宽度 */
const PAGE_WIDTH = 794
const PAGE_HEIGHT = 1123

const wrapRef = ref(null)
const scale = ref(0.3)
let observer = null

/** 按容器实际宽度计算缩放比例 */
const measure = () => {
  if (wrapRef.value) scale.value = wrapRef.value.clientWidth / PAGE_WIDTH
}

onMounted(() => {
  measure()
  observer = new ResizeObserver(measure)
  observer.observe(wrapRef.value)
})

onBeforeUnmount(() => observer?.disconnect())
</script>

<template>
  <div ref="wrapRef" class="tpl-preview" :class="`tpl-preview-${props.size}`" :style="{ height: `${PAGE_HEIGHT * scale}px` }">
    <div class="tpl-page" :style="{ transform: `scale(${scale})`, background: props.pageStyle?.background || '#ffffff' }">
      <div
        v-for="component in props.components"
        :key="component.id"
        class="tpl-block"
        :class="{ structural: ['divider', 'avatar', 'contact', 'block', 'progress', 'rating', 'tag', 'qrcode', 'image', 'radar', 'ring', 'gauge', 'timeline', 'wordcloud', 'barchart', 'statcard'].includes(component.type) }"
        :style="buildBlockStyle(component)"
      >
        <div v-if="component.type === 'divider'" class="resume-divider" :style="buildDividerStyle(component)"></div>
        <div v-else-if="component.type === 'avatar'" class="resume-avatar" :style="buildAvatarStyle(component)">
          <img v-if="component.src" :src="component.src" :alt="component.content || '简历头像'" />
          <span v-else>{{ component.content || '头像' }}</span>
        </div>
        <div v-else-if="component.type === 'contact'" class="resume-contact" :style="buildComponentStyle(component)">
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
        <div v-else-if="component.type === 'block'"></div>
        <div v-else-if="component.type === 'progress'" class="resume-progress">
          <div :style="buildProgressStyle(component).label">
            <span>{{ component.content }}</span>
            <span>{{ component.style?.percent ?? 60 }}%</span>
          </div>
          <div :style="buildProgressStyle(component).track">
            <div :style="buildProgressStyle(component).fill"></div>
          </div>
        </div>
        <div v-else-if="component.type === 'rating'" class="resume-rating">
          <span :style="buildRatingStyle(component).label">{{ component.content }}</span>
          <span>
            <span
              v-for="i in component.style?.total || 5"
              :key="i"
              :style="buildRatingStyle(component).item"
            >{{ i <= (component.style?.stars || 0) ? (component.style?.shape === 'dot' ? '●' : '★') : (component.style?.shape === 'dot' ? '○' : '☆') }}</span>
          </span>
        </div>
        <span v-else-if="component.type === 'tag'" :style="buildTagStyle(component)">{{ component.content }}</span>
        <div v-else-if="component.type === 'qrcode'" :style="buildQrcodeStyle(component)">
          <span class="qrcode-label">{{ component.content }}</span>
        </div>
        <div v-else-if="component.type === 'image'" :style="buildImageStyle(component)">
          <img v-if="component.src" :src="component.src" :alt="component.content" />
          <span v-else>{{ component.content || '图片' }}</span>
        </div>
        <ResumeVisual v-else-if="isVisualComponent(component)" :component="component" />
        <div v-else class="tpl-block-content" :style="buildComponentStyle(component)">{{ component.content }}</div>
      </div>
    </div>
  </div>
</template>
