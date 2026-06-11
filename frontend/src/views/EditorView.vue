<!--
  简历编辑器页面
  功能：参考 Canva 的编辑器布局——顶部上下文工具栏（标题、字号、加粗、颜色、对齐、缩放、保存状态），
        左侧素材面板（组件库 / 模板 / AI 优化三个标签），中间 A4 画布
  说明：组件支持点选、拖拽、缩放、双击编辑、复制、删除；内容变更后自动保存草稿；
        高级组件 / 会员模板仅展示标记，不做权限拦截【会员权限校验预留】
-->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import DragResumeCanvas from '../components/drag-resume/DragResumeCanvas.vue'
import TemplatePreview from '../components/template-preview/TemplatePreview.vue'
import MemberUpgradeDialog from '../components/member-tip/MemberUpgradeDialog.vue'
import { listResumes, saveResume } from '../api/resume'
import { listTemplates, getTemplateVipConfig } from '../api/template'
import { createPaymentOrder, listMemberPackages, mockPayOrder } from '../api/member'
import { getUserSystemConfig } from '../api/user'
import { optimizeResume } from '../api/ai'
import { recordExport } from '../api/export'
import { useUserStore } from '../store/user'
import { CONTACT_ICON_MAP, getContactIcon, isTextComponent } from '../utils/componentStyle'
import { COMPONENT_TREE, flattenComponents } from '../data/componentLibrary'

const userStore = useUserStore()
const currentResume = ref(null)
const templates = ref([])
const vipComponentGroups = ref([])
const packages = ref([])
const upgradeVisible = ref(false)
const loadingPackageId = ref(null)
const systemConfig = ref({ paymentEnabled: false, mockPaymentEnabled: true })
const selectedId = ref('')
const activeTab = ref('components')
const zoom = ref(1)
const aiResult = ref('')
const aiLoading = ref(false)
/** 保存状态：saved-已保存 dirty-有改动 saving-保存中 */
const saveState = ref('saved')
/** 自动保存防抖定时器 */
let saveTimer = null
/** 初始化加载阶段跳过自动保存 */
let suppressAutosave = true

/** 左侧组件库：48 个内置组件，按 7 大分类组织 */
const componentTree = COMPONENT_TREE
/** 组件搜索关键字 */
const librarySearch = ref('')
/** 当前展开的分类 keys */
const expandedGroups = ref(['text', 'avatar'])

/** 切换分类展开 */
const toggleGroup = (key) => {
  const idx = expandedGroups.value.indexOf(key)
  if (idx >= 0) expandedGroups.value.splice(idx, 1)
  else expandedGroups.value.push(key)
}

/** 搜索过滤后的扁平结果 */
const searchedComponents = computed(() => {
  const kw = librarySearch.value.trim().toLowerCase()
  if (!kw) return null
  return flattenComponents().filter((c) => c.label.toLowerCase().includes(kw) || c.groupLabel.toLowerCase().includes(kw))
})

const contactIconOptions = Object.entries(CONTACT_ICON_MAP).map(([value, icon]) => ({
  value,
  label: icon.label
}))

/** 判断当前用户是否拥有会员权益 */
const isVipUser = () => userStore.profile?.vipLevel && userStore.profile.vipLevel !== 'FREE'

/** 判断组件是否命中后台配置的会员分组 */
const isVipComponent = (item) => item?.vipOnly || vipComponentGroups.value.includes(item?.groupKey)

/** 普通用户触发会员能力时展示升级弹窗 */
const requireVip = () => {
  upgradeVisible.value = true
  return false
}

/** 当前选中的组件对象 */
const selectedComponent = computed(() =>
  currentResume.value?.components?.find((item) => item.id === selectedId.value) || null
)

/** 当前简历页面级样式，至少保证背景色始终可编辑 */
const resumePageStyle = computed(() => {
  if (!currentResume.value) return { background: '#ffffff' }
  currentResume.value.style = currentResume.value.style || { background: '#ffffff' }
  if (!currentResume.value.style.background) currentResume.value.style.background = '#ffffff'
  return currentResume.value.style
})

/** 选中组件是否支持文字样式编辑 */
const textEditable = computed(() => isTextComponent(selectedComponent.value))
/** 选中组件是否为分割线 */
const dividerSelected = computed(() => selectedComponent.value?.type === 'divider')
/** 选中组件是否为头像 */
const avatarSelected = computed(() => selectedComponent.value?.type === 'avatar')
/** 选中组件是否为进度条 */
const progressSelected = computed(() => selectedComponent.value?.type === 'progress')
/** 选中组件是否为联系方式 */
const contactSelected = computed(() => selectedComponent.value?.type === 'contact')

onMounted(async () => {
  await userStore.loadProfile()
  const [resumes, templateList, vipConfig, packageList, config] = await Promise.all([
    listResumes({ userId: userStore.profile?.id || 1 }),
    listTemplates({ categoryCode: '' }),
    getTemplateVipConfig(),
    listMemberPackages(),
    getUserSystemConfig()
  ])
  currentResume.value = ensureResumeStyle(resumes[0])
  templates.value = templateList.map(ensureResumeStyle)
  vipComponentGroups.value = vipConfig?.vipComponentGroups || []
  packages.value = packageList
  systemConfig.value = config || systemConfig.value
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  clearTimeout(saveTimer)
})

/**
 * 确保简历对象具备页面级样式字段，兼容历史数据
 */
const ensureResumeStyle = (resume) => {
  if (!resume) return resume
  return {
    ...resume,
    style: {
      background: '#ffffff',
      ...(resume.style || {})
    }
  }
}

/**
 * 选中 / 取消选中组件，并保证组件样式对象存在，便于工具栏直接绑定
 */
const handleSelect = (id) => {
  selectedId.value = id
  const component = selectedComponent.value
  if (component && !component.style) component.style = {}
}

/**
 * 画布数据变更入口：标记为有改动并触发防抖自动保存
 */
const markDirty = () => {
  if (suppressAutosave) return
  saveState.value = 'dirty'
  clearTimeout(saveTimer)
  saveTimer = setTimeout(() => handleSave(true), 1500)
}

// 深度监听简历数据：标题、组件内容、样式任一变化都进入自动保存流程
watch(
  () => currentResume.value,
  () => {
    if (suppressAutosave && currentResume.value) {
      // 首次加载完成后开启自动保存
      suppressAutosave = false
      return
    }
    markDirty()
  },
  { deep: true }
)

/**
 * 保存简历草稿
 * @param silent 是否静默保存（自动保存时不弹提示）
 */
const handleSave = async (silent = false) => {
  if (!currentResume.value) return
  saveState.value = 'saving'
  const saved = await saveResume({
    id: currentResume.value.id,
    title: currentResume.value.title,
    targetJob: currentResume.value.targetJob,
    templateId: currentResume.value.templateId,
    draft: true,
    components: currentResume.value.components,
    style: currentResume.value.style,
    userId: userStore.profile?.id || 1
  })
  // 回写后端返回值会再次触发深度监听，这里只同步 id，避免无限保存循环
  currentResume.value.id = saved.id
  saveState.value = 'saved'
  if (!silent) ElMessage.success('草稿已保存')
}

/**
 * 新增组件（点击组件库或拖入画布）
 * @param item 组件库条目或携带落点坐标的拖拽数据
 */
const addComponent = (item) => {
  const components = currentResume.value?.components
  if (!components) return
  if (isVipComponent(item) && !isVipUser()) {
    requireVip()
    return
  }
  const component = {
    ...JSON.parse(JSON.stringify(item)),
    vipOnly: isVipComponent(item),
    id: `c${Date.now()}`,
    x: item.x ?? 48,
    y: item.y ?? Math.max(...components.map((block) => (block.y || 0) + (block.height || 60)), 24) + 24
  }
  components.push(component)
  handleSelect(component.id)
}

/**
 * 复制当前选中组件（错开 16px 放置）
 */
const duplicateSelected = () => {
  const component = selectedComponent.value
  if (!component) return
  addComponent({ ...JSON.parse(JSON.stringify(component)), x: (component.x || 0) + 16, y: (component.y || 0) + 16 })
}

/**
 * 删除当前选中组件
 */
const removeSelected = () => {
  const components = currentResume.value?.components
  if (!components || !selectedId.value) return
  const index = components.findIndex((item) => item.id === selectedId.value)
  if (index >= 0) components.splice(index, 1)
  selectedId.value = ''
}

/**
 * 键盘删除：Delete / Backspace 移除选中组件（输入框、文本域聚焦时不触发）
 */
const onKeydown = (event) => {
  const tag = event.target?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA') return
  if ((event.key === 'Delete' || event.key === 'Backspace') && selectedId.value) {
    event.preventDefault()
    removeSelected()
  }
}

/**
 * 上传头像图片：转换为 dataURL 存到组件 src，保证草稿保存和预览都能直接渲染
 */
const handleAvatarFileChange = (event) => {
  const file = event.target.files?.[0]
  if (!file || !selectedComponent.value) return
  const reader = new FileReader()
  reader.onload = () => {
    selectedComponent.value.src = reader.result
    selectedComponent.value.content = '头像'
    markDirty()
  }
  reader.readAsDataURL(file)
  event.target.value = ''
}

/**
 * 设置选中组件的文字对齐方式
 */
const setAlign = (value) => {
  if (selectedComponent.value) selectedComponent.value.style.textAlign = value
}

/**
 * 切换选中组件加粗
 */
const toggleBold = () => {
  const style = selectedComponent.value?.style
  if (style) style.fontWeight = style.fontWeight >= 600 ? 400 : 700
}

/**
 * 套用模板：将模板组件深拷贝替换当前画布内容
 */
const applyTemplateToCanvas = (template) => {
  if (!currentResume.value) return
  if (template.vipTemplate && !isVipUser()) {
    requireVip()
    return
  }
  currentResume.value.templateId = template.id
  currentResume.value.components = JSON.parse(JSON.stringify(template.components || []))
  currentResume.value.style = { background: '#ffffff', ...(template.style || {}) }
  selectedId.value = ''
  ElMessage.success(`已套用「${template.name}」`)
}

const handleBuy = async (item) => {
  if (!systemConfig.value.paymentEnabled) {
    ElMessage.warning('支付功能暂未开启，请联系管理员')
    return
  }
  loadingPackageId.value = item.id
  try {
    const order = await createPaymentOrder({ userId: userStore.profile?.id || 1, packageId: item.id, payChannel: 'MOCK' })
    if (systemConfig.value.mockPaymentEnabled) {
      await mockPayOrder(order.orderNo, { userId: userStore.profile?.id || 1 })
      ElMessage.success(`模拟支付成功，已开通${item.name}`)
      await userStore.loadProfile()
      upgradeVisible.value = false
    } else {
      ElMessage.success('订单已创建，请等待支付功能开放')
    }
  } finally {
    loadingPackageId.value = null
  }
}

/**
 * 调用 AI 润色：后端代理请求，前端不接触 API Key
 */
const handleAi = async () => {
  if (!currentResume.value) return
  aiLoading.value = true
  try {
    const content = currentResume.value.components.map((item) => item.content).join('\n')
    const result = await optimizeResume({ featureType: 'POLISH', content, jobDescription: currentResume.value.targetJob, userId: userStore.profile?.id || 1 })
    aiResult.value = result.optimizedContent
  } finally {
    aiLoading.value = false
  }
}

/**
 * 将 AI 优化结果替换到选中组件内容
 */
const applyAiResult = () => {
  if (selectedComponent.value && aiResult.value) {
    selectedComponent.value.content = aiResult.value
    ElMessage.success('已替换选中组件内容')
  }
}

/**
 * 导出 PDF：记录导出行为（额度统计预留）后调起浏览器打印，仅打印简历纸张
 */
const handleExport = async () => {
  await recordExport({ userId: userStore.profile?.id || 1, resumeId: currentResume.value?.id || 1, exportType: 'PDF', highDefinition: true })
  selectedId.value = ''
  window.print()
}

/** 缩放步进控制，范围 50% - 150% */
const zoomBy = (delta) => {
  zoom.value = Math.round(Math.min(Math.max(zoom.value + delta, 0.5), 1.5) * 10) / 10
}
</script>

<template>
  <div v-if="currentResume" class="editor-wrap">
    <!-- 顶部工具栏：左侧标题，中间选中组件的上下文样式控件，右侧保存与导出 -->
    <div class="editor-toolbar card">
      <el-input v-model="currentResume.title" class="title-input" placeholder="简历标题" />

      <el-divider direction="vertical" />

      <template v-if="selectedComponent">
        <template v-if="textEditable || contactSelected">
          <span class="toolbar-label muted">字号</span>
          <el-input-number
            v-model="selectedComponent.style.fontSize"
            :min="12"
            :max="60"
            size="small"
            controls-position="right"
            style="width: 96px"
          />
          <button
            class="tool-button"
            :class="{ active: (selectedComponent.style.fontWeight || 400) >= 600 }"
            title="加粗"
            @click="toggleBold"
          >B</button>
          <span class="toolbar-label muted">颜色</span>
          <el-color-picker v-model="selectedComponent.style.color" size="small" />
        </template>

        <template v-if="textEditable">
          <span class="toolbar-label muted">底色</span>
          <el-color-picker v-model="selectedComponent.style.background" size="small" />
          <div class="tool-group">
            <button class="tool-button" :class="{ active: (selectedComponent.style.textAlign || 'left') === 'left' }" title="左对齐" @click="setAlign('left')">⇤</button>
            <button class="tool-button" :class="{ active: selectedComponent.style.textAlign === 'center' }" title="居中" @click="setAlign('center')">⇆</button>
            <button class="tool-button" :class="{ active: selectedComponent.style.textAlign === 'right' }" title="右对齐" @click="setAlign('right')">⇥</button>
          </div>
        </template>

        <template v-if="dividerSelected">
          <span class="toolbar-label muted">线条颜色</span>
          <el-color-picker v-model="selectedComponent.style.borderColor" size="small" />
          <span class="toolbar-label muted">粗细</span>
          <el-input-number
            v-model="selectedComponent.style.lineWidth"
            :min="1"
            :max="12"
            size="small"
            controls-position="right"
            style="width: 88px"
          />
        </template>

        <template v-if="progressSelected">
          <span class="toolbar-label muted">名称</span>
          <el-input v-model="selectedComponent.content" size="small" style="width: 120px" />
          <span class="toolbar-label muted">进度</span>
          <el-input-number
            v-model="selectedComponent.style.percent"
            :min="0"
            :max="100"
            size="small"
            controls-position="right"
            style="width: 96px"
          />
          <span class="toolbar-label muted">进度色</span>
          <el-color-picker v-model="selectedComponent.style.color" size="small" />
          <span class="toolbar-label muted">底色</span>
          <el-color-picker v-model="selectedComponent.style.background" size="small" />
        </template>

        <template v-if="contactSelected">
          <span class="toolbar-label muted">图标</span>
          <el-select v-model="selectedComponent.style.icon" size="small" style="width: 92px">
            <el-option
              v-for="item in contactIconOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </template>

        <template v-if="avatarSelected">
          <span class="toolbar-label muted">头像背景</span>
          <el-color-picker v-model="selectedComponent.style.background" size="small" />
          <span class="toolbar-label muted">占位色</span>
          <el-color-picker v-model="selectedComponent.style.color" size="small" />
          <span class="toolbar-label muted">头像描边</span>
          <el-color-picker v-model="selectedComponent.style.borderColor" size="small" />
          <span class="toolbar-label muted">描边粗细</span>
          <el-input-number
            v-model="selectedComponent.style.borderWidth"
            :min="0"
            :max="8"
            size="small"
            controls-position="right"
            style="width: 88px"
          />
          <label class="avatar-upload-button">
            上传头像
            <input type="file" accept="image/*" @change="handleAvatarFileChange" />
          </label>
        </template>

        <el-divider direction="vertical" />
        <el-button size="small" @click="duplicateSelected">复制</el-button>
        <el-button size="small" type="danger" plain @click="removeSelected">删除</el-button>
      </template>
      <span v-else class="muted toolbar-hint">选中画布中的组件可设置样式，双击可编辑文字</span>

      <el-divider direction="vertical" />
      <span class="toolbar-label muted">简历背景</span>
      <el-color-picker v-model="resumePageStyle.background" size="small" />

      <span class="toolbar-spacer"></span>

      <div class="zoom-controls">
        <button class="tool-button" @click="zoomBy(-0.1)">−</button>
        <span class="zoom-value">{{ Math.round(zoom * 100) }}%</span>
        <button class="tool-button" @click="zoomBy(0.1)">＋</button>
      </div>
      <span class="save-status muted">
        {{ saveState === 'saving' ? '保存中…' : saveState === 'dirty' ? '有未保存改动' : '已自动保存' }}
      </span>
      <el-button size="small" @click="handleSave(false)">保存</el-button>
      <el-button size="small" type="primary" @click="handleExport">导出 PDF</el-button>
    </div>

    <div class="editor-body">
      <!-- 左侧素材面板：组件 / 模板 / AI 三个标签 -->
      <aside class="side-panel card">
        <div class="panel-tabs">
          <button class="panel-tab" :class="{ active: activeTab === 'components' }" @click="activeTab = 'components'">组件</button>
          <button class="panel-tab" :class="{ active: activeTab === 'templates' }" @click="activeTab = 'templates'">模板</button>
          <button class="panel-tab" :class="{ active: activeTab === 'ai' }" @click="activeTab = 'ai'">AI 优化</button>
        </div>

        <!-- 组件库：树状分类 + 搜索框，点击添加或拖到画布 -->
        <div v-if="activeTab === 'components'">
          <div class="library-search">
            <el-input v-model="librarySearch" placeholder="搜索组件..." size="small" clearable />
          </div>

          <!-- 搜索结果：扁平展示 -->
          <div v-if="searchedComponents" class="library-grid">
            <div
              v-for="item in searchedComponents"
              :key="`s-${item.groupKey}-${item.label}`"
              class="library-card"
              :class="{ vip: isVipComponent(item) }"
              draggable="true"
              @dragstart="$event.dataTransfer.setData('application/json', JSON.stringify(item))"
              @click="addComponent(item)"
            >
              <div class="library-card-icon">{{ item.label.slice(0, 1) }}</div>
              <div class="library-card-label">{{ item.label }}</div>
              <span v-if="isVipComponent(item)" class="library-vip-mark">会员</span>
            </div>
          </div>

          <!-- 按分类树状展示 -->
          <div v-else class="library-tree">
            <div v-for="group in componentTree" :key="group.key" class="library-group">
              <button class="library-group-header" @click="toggleGroup(group.key)">
                <span class="library-group-arrow" :class="{ open: expandedGroups.includes(group.key) }">▸</span>
                <span class="library-group-icon">{{ group.icon }}</span>
                <span class="library-group-label">{{ group.label }}</span>
                <span class="library-group-count">{{ group.children.length }}</span>
              </button>
              <div v-show="expandedGroups.includes(group.key)" class="library-grid">
                <div
                  v-for="item in group.children"
                  :key="`${group.key}-${item.label}`"
                  class="library-card"
                  :class="{ vip: isVipComponent({ ...item, groupKey: group.key }) }"
                  draggable="true"
                  @dragstart="$event.dataTransfer.setData('application/json', JSON.stringify({ ...item, groupKey: group.key, groupLabel: group.label }))"
                  @click="addComponent({ ...item, groupKey: group.key, groupLabel: group.label })"
                >
                  <div class="library-card-icon">{{ item.label.slice(0, 1) }}</div>
                  <div class="library-card-label">{{ item.label }}</div>
                  <span v-if="isVipComponent({ ...item, groupKey: group.key })" class="library-vip-mark">会员</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 模板：缩略图预览，点击一键替换画布内容 -->
        <div v-if="activeTab === 'templates'">
          <p class="muted panel-hint">套用模板会替换当前画布内容</p>
          <div v-for="item in templates" :key="item.id" class="panel-template" @click="applyTemplateToCanvas(item)">
            <TemplatePreview :components="item.components" :page-style="item.style" size="compact" />
            <div class="panel-template-name">
              <span>{{ item.name }}</span>
              <span v-if="item.vipTemplate" class="vip-badge">会员</span>
            </div>
          </div>
        </div>

        <!-- AI 优化：润色全文，可替换到选中组件 -->
        <div v-if="activeTab === 'ai'">
          <el-button type="primary" class="full-button" :loading="aiLoading" @click="handleAi">AI 润色全文</el-button>
          <el-button v-if="aiResult && selectedComponent" class="full-button" @click="applyAiResult">替换选中组件内容</el-button>
          <div class="ai-result">{{ aiResult || '点击「AI 润色全文」，优化建议会显示在这里。' }}</div>
        </div>
      </aside>

      <!-- 中间画布 -->
      <DragResumeCanvas
        :components="currentResume.components"
        :page-style="resumePageStyle"
        :selected-id="selectedId"
        :zoom="zoom"
        @select="handleSelect"
        @change="markDirty"
        @add="addComponent"
      />
    </div>
  </div>

  <MemberUpgradeDialog
    v-model:visible="upgradeVisible"
    :packages="packages"
    :payment-enabled="systemConfig.paymentEnabled"
    :mock-payment-enabled="systemConfig.mockPaymentEnabled"
    :loading-package-id="loadingPackageId"
    @buy="handleBuy"
  />
</template>
