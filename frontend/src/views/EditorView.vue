<!--
  简历编辑器页面
  功能：参考 Canva 的编辑器布局——顶部上下文工具栏（标题、字号、加粗、颜色、对齐、缩放、保存状态），
        左侧素材面板（组件库 / 模板 / AI 优化三个标签），中间 A4 画布
  说明：组件支持点选、拖拽、缩放、双击编辑、复制、删除；内容变更后自动保存草稿；
        高级组件 / 会员模板仅展示标记，不做权限拦截【会员权限校验预留】
-->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { toPng } from 'html-to-image'
import DragResumeCanvas from '../components/drag-resume/DragResumeCanvas.vue'
import TemplatePreview from '../components/template-preview/TemplatePreview.vue'
import MemberUpgradeDialog from '../components/member-tip/MemberUpgradeDialog.vue'
import {
  listResumes,
  saveResume,
  createBlankResume,
  copyResume,
  deleteResume,
  publishResume,
  listResumeVersions,
  restoreResumeVersion,
  createResumeShare,
  getResumeShare
} from '../api/resume'
import { listTemplates, getTemplateVipConfig } from '../api/template'
import { listMemberPackages } from '../api/member'
import { getUserSystemConfig } from '../api/user'
import { optimizeResume } from '../api/ai'
import { recordExport } from '../api/export'
import { useUserStore } from '../store/user'
import { CONTACT_ICON_MAP, getContactIcon, isTextComponent, isVisualComponent } from '../utils/componentStyle'
import { COMPONENT_TREE, flattenComponents } from '../data/componentLibrary'
import VisualEditor from '../components/drag-resume/VisualEditor.vue'

const route = useRoute()
const userStore = useUserStore()
const currentResume = ref(null)
const myResumes = ref([])
const templates = ref([])
const vipComponentGroups = ref([])
/** 后台配置的会员专属单个组件 key（格式 groupKey:label），优先级高于分组 */
const vipComponentKeys = ref([])
const packages = ref([])
const upgradeVisible = ref(false)
/** 图表数据编辑抽屉显隐 */
const visualEditorVisible = ref(false)
const systemConfig = ref({ paymentEnabled: false, mockPaymentEnabled: true })
const selectedId = ref('')
const activeTab = ref('components')
const zoom = ref(1)
const aiResult = ref('')
const aiLoading = ref(false)
const aiScore = ref(null)
const aiSuggestions = ref([])
const aiJobDescription = ref('')
/** 版本历史抽屉 */
const versionDrawerVisible = ref(false)
const versions = ref([])
const versionLoading = ref(false)
/** 分享弹窗 */
const shareVisible = ref(false)
const shareInfo = ref(null)
const shareLoading = ref(false)
/** 导出中状态 */
const exporting = ref(false)
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

/** 组件唯一 key：分组 + 名称，用于单组件级会员标记 */
const componentKeyOf = (item) => `${item?.groupKey || ''}:${item?.label || ''}`

/** 判断组件是否需要会员：组件自带 vipOnly、命中单组件 key、或命中整组配置 */
const isVipComponent = (item) =>
  item?.vipOnly
  || vipComponentKeys.value.includes(componentKeyOf(item))
  || vipComponentGroups.value.includes(item?.groupKey)

/** 组件是否对当前用户锁定（会员专属且当前非会员）：用于灰显 + 禁止拖拽 */
const lockedForUser = (item) => isVipComponent(item) && !isVipUser()

/** 拖拽开始：锁定组件直接阻止拖拽并弹升级，否则写入拖拽数据 */
const onLibraryDragStart = (event, item) => {
  if (lockedForUser(item)) {
    event.preventDefault()
    requireVip()
    return
  }
  event.dataTransfer.setData('application/json', JSON.stringify(item))
}

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
/** 选中组件是否为会员高级可视化组件（雷达图/环形图/时间线/词云等） */
const visualSelected = computed(() => isVisualComponent(selectedComponent.value))
/** 打开图表数据编辑抽屉 */
const openVisualEditor = () => {
  if (visualSelected.value) visualEditorVisible.value = true
}

onMounted(async () => {
  await userStore.loadProfile()
  const [resumes, templateList, vipConfig, packageList, config] = await Promise.all([
    listResumes({ userId: userStore.profile?.id || 1 }),
    listTemplates({ categoryCode: '' }),
    getTemplateVipConfig(),
    listMemberPackages(),
    getUserSystemConfig()
  ])
  myResumes.value = (resumes || []).map(ensureResumeStyle)
  // 支持 /editor?id=xxx 打开指定简历，否则默认第一份；如果没有任何简历则自动创建空白简历
  const wantId = route.query.id ? Number(route.query.id) : null
  let target = (wantId && myResumes.value.find((r) => r.id === wantId)) || myResumes.value[0]
  if (!target) {
    // 没有历史简历时自动创建一份空白简历
    const blank = await createBlankResume({ userId: userStore.profile?.id || 1 })
    target = ensureResumeStyle(blank)
    myResumes.value.push(target)
  }
  currentResume.value = ensureResumeStyle(target)
  templates.value = templateList.map(ensureResumeStyle)
  vipComponentGroups.value = vipConfig?.vipComponentGroups || []
  vipComponentKeys.value = vipConfig?.vipComponentKeys || []
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
    // 保留当前简历的草稿/正式状态：正式简历编辑后仍为正式，不会被自动保存打回草稿
    draft: currentResume.value.draft !== false,
    components: currentResume.value.components,
    style: currentResume.value.style,
    userId: userStore.profile?.id || 1
  })
  // 回写后端返回值会再次触发深度监听，这里只同步 id，避免无限保存循环
  currentResume.value.id = saved.id
  // 同步到我的简历列表，保证下拉切换的标题等信息最新
  syncResumeToList(saved)
  saveState.value = 'saved'
  if (!silent) ElMessage.success('草稿已保存')
}

/** 将保存结果同步到 myResumes 列表（存在则更新，不存在则插入到最前） */
const syncResumeToList = (saved) => {
  if (!saved) return
  const idx = myResumes.value.findIndex((r) => r.id === saved.id)
  const merged = {
    id: saved.id,
    title: currentResume.value.title,
    targetJob: currentResume.value.targetJob,
    templateId: currentResume.value.templateId
  }
  if (idx >= 0) myResumes.value[idx] = { ...myResumes.value[idx], ...merged }
  else myResumes.value.unshift(merged)
}

/** 切换到另一份简历（先保存当前再加载目标） */
const switchResume = async (id) => {
  if (!id || (currentResume.value && id === currentResume.value.id)) return
  if (saveState.value !== 'saved') await handleSave(true)
  const list = await listResumes({ userId: userStore.profile?.id || 1 })
  myResumes.value = (list || []).map(ensureResumeStyle)
  const target = myResumes.value.find((r) => r.id === id)
  if (target) {
    suppressAutosave = true
    selectedId.value = ''
    currentResume.value = ensureResumeStyle(target)
  }
}

/** 新建空白简历并切换过去 */
const handleNewResume = async () => {
  if (saveState.value !== 'saved') await handleSave(true)
  const blank = await createBlankResume({ userId: userStore.profile?.id || 1 })
  suppressAutosave = true
  selectedId.value = ''
  currentResume.value = ensureResumeStyle(blank)
  myResumes.value.unshift({ id: blank.id, title: blank.title, targetJob: blank.targetJob })
  ElMessage.success('已新建空白简历')
}

/** 复制当前简历 */
const handleCopyResume = async () => {
  if (!currentResume.value) return
  if (saveState.value !== 'saved') await handleSave(true)
  const copy = await copyResume(currentResume.value.id, { userId: userStore.profile?.id || 1 })
  suppressAutosave = true
  selectedId.value = ''
  currentResume.value = ensureResumeStyle(copy)
  myResumes.value.unshift({ id: copy.id, title: copy.title, targetJob: copy.targetJob })
  ElMessage.success('已复制为新简历')
}

/** 删除当前简历 */
const handleDeleteResume = async () => {
  if (!currentResume.value) return
  if (myResumes.value.length <= 1) {
    ElMessage.warning('至少保留一份简历')
    return
  }
  await ElMessageBox.confirm(`确定删除「${currentResume.value.title}」吗？`, '删除确认', { type: 'warning' })
  const deletedId = currentResume.value.id
  await deleteResume(deletedId, { userId: userStore.profile?.id || 1 })
  myResumes.value = myResumes.value.filter((r) => r.id !== deletedId)
  const next = myResumes.value[0]
  if (next) {
    const list = await listResumes({ userId: userStore.profile?.id || 1 })
    myResumes.value = (list || []).map(ensureResumeStyle)
    suppressAutosave = true
    selectedId.value = ''
    currentResume.value = ensureResumeStyle(myResumes.value.find((r) => r.id === next.id) || myResumes.value[0])
  }
  ElMessage.success('简历已删除')
}

/* ===== 草稿 / 发布 ===== */
/** 当前简历是否为草稿（draft 字段为 false 才算正式） */
const isDraft = computed(() => currentResume.value?.draft !== false)

/** 发布当前简历：先保存最新内容，再调用发布接口转为正式简历 */
const handlePublish = async () => {
  if (!currentResume.value) return
  if (saveState.value !== 'saved') await handleSave(true)
  await publishResume(currentResume.value.id, { userId: userStore.profile?.id || 1 })
  currentResume.value.draft = false
  ElMessage.success('已发布为正式简历')
}

/* ===== 版本历史 ===== */
const openVersions = async () => {
  if (!currentResume.value) return
  versionDrawerVisible.value = true
  versionLoading.value = true
  try {
    versions.value = await listResumeVersions(currentResume.value.id) || []
  } finally {
    versionLoading.value = false
  }
}

const handleRestoreVersion = async (version) => {
  await ElMessageBox.confirm('回滚后将用该历史版本覆盖当前内容（当前内容会另存为一条历史），确定继续？', '回滚确认', { type: 'warning' })
  const restored = await restoreResumeVersion(currentResume.value.id, version.id)
  suppressAutosave = true
  selectedId.value = ''
  currentResume.value = ensureResumeStyle(restored)
  versionDrawerVisible.value = false
  ElMessage.success('已回滚到该版本')
}

/* ===== 分享 ===== */
const shareLink = computed(() => {
  if (!shareInfo.value?.token) return ''
  return `${window.location.origin}/s/${shareInfo.value.token}`
})

const openShare = async () => {
  if (!currentResume.value) return
  if (saveState.value !== 'saved') await handleSave(true)
  shareVisible.value = true
  shareLoading.value = true
  try {
    // 先生成/获取分享，再取最新浏览量
    shareInfo.value = await createResumeShare(currentResume.value.id)
    const latest = await getResumeShare(currentResume.value.id)
    if (latest) shareInfo.value = latest
  } finally {
    shareLoading.value = false
  }
}

const copyShareLink = async () => {
  if (!shareLink.value) return
  try {
    await navigator.clipboard.writeText(shareLink.value)
    ElMessage.success('链接已复制')
  } catch (e) {
    ElMessage.warning('复制失败，请手动复制')
  }
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
  ElMessage.success('组件已复制')
}

/**
 * 删除当前选中组件
 */
const removeSelected = async () => {
  const components = currentResume.value?.components
  if (!components || !selectedId.value) return

  const component = components.find((item) => item.id === selectedId.value)
  if (!component) return

  try {
    await ElMessageBox.confirm(`确定删除「${component.label}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    const index = components.findIndex((item) => item.id === selectedId.value)
    if (index >= 0) components.splice(index, 1)
    selectedId.value = ''
    ElMessage.success('组件已删除')
  } catch {
    // 用户取消删除
  }
}

/**
 * 键盘删除：Delete / Backspace 移除选中组件（输入框、文本域聚焦时不触发）
 */
const onKeydown = (event) => {
  const tag = event.target?.tagName
  const isInput = tag === 'INPUT' || tag === 'TEXTAREA'

  // Ctrl+S / Cmd+S: 保存
  if ((event.ctrlKey || event.metaKey) && event.key === 's') {
    event.preventDefault()
    handleSave(false)
    return
  }

  // Ctrl+P / Cmd+P: 预览（导出 PDF）
  if ((event.ctrlKey || event.metaKey) && event.key === 'p') {
    event.preventDefault()
    handleExport('pdf')
    return
  }

  // Delete / Backspace: 删除选中组件（不在输入框内时）
  if (!isInput && (event.key === 'Delete' || event.key === 'Backspace') && selectedId.value) {
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



/**
 * 调用 AI 能力：润色 / 岗位适配 / 中英翻译，后端代理请求，前端不接触 API Key
 * @param featureType POLISH | JOB_MATCH | TRANSLATE
 */
const handleAi = async (featureType = 'POLISH') => {
  if (!currentResume.value) return
  aiLoading.value = true
  aiScore.value = null
  aiSuggestions.value = []
  try {
    // 优先使用选中组件内容，否则取全文
    const content = selectedComponent.value?.content
      || currentResume.value.components.map((item) => item.content).filter(Boolean).join('\n')
    const jobDescription = featureType === 'JOB_MATCH'
      ? (aiJobDescription.value || currentResume.value.targetJob)
      : currentResume.value.targetJob
    const result = await optimizeResume({
      featureType,
      content,
      jobDescription,
      userId: userStore.profile?.id || 1
    })
    aiResult.value = result.optimizedContent
    aiScore.value = result.score ?? null
    aiSuggestions.value = result.suggestions || []
    ElMessage.success('AI 优化完成')
  } catch (error) {
    ElMessage.error('AI 请求失败，请稍后重试')
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
 * 导出：PDF（打印）/ PNG（图片）/ Word（doc）
 * @param format pdf | png | word
 */
const handleExport = async (format = 'pdf') => {
  if (!currentResume.value) return
  selectedId.value = ''
  const userId = userStore.profile?.id || 1
  const resumeId = currentResume.value.id || 1
  const fileBase = (currentResume.value.title || '我的简历').replace(/[\\/:*?"<>|]/g, '_')
  if (format === 'pdf') {
    await recordExport({ userId, resumeId, exportType: 'PDF', highDefinition: true })
    window.print()
    return
  }
  if (format === 'png') {
    exporting.value = true
    try {
      const node = document.querySelector('.resume-page')
      if (!node) return
      const dataUrl = await toPng(node, { pixelRatio: 2, backgroundColor: '#ffffff', cacheBust: true })
      triggerDownload(dataUrl, `${fileBase}.png`)
      await recordExport({ userId, resumeId, exportType: 'PNG', highDefinition: true })
      ElMessage.success('已导出 PNG 图片')
    } catch (e) {
      ElMessage.error('图片导出失败，请重试')
    } finally {
      exporting.value = false
    }
    return
  }
  if (format === 'word') {
    const node = document.querySelector('.resume-page')
    if (!node) return
    // 将简历纸张 HTML 包成 Word 可识别的文档
    const html = `<html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>`
      + `<head><meta charset='utf-8'><title>${fileBase}</title></head><body>${node.outerHTML}</body></html>`
    const blob = new Blob(['﻿', html], { type: 'application/msword' })
    triggerDownload(URL.createObjectURL(blob), `${fileBase}.doc`)
    await recordExport({ userId, resumeId, exportType: 'WORD', highDefinition: false })
    ElMessage.success('已导出 Word 文档')
  }
}

/** 触发浏览器下载 */
const triggerDownload = (href, filename) => {
  const link = document.createElement('a')
  link.href = href
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

/** 缩放步进控制，范围 50% - 150% */
const zoomBy = (delta) => {
  zoom.value = Math.round(Math.min(Math.max(zoom.value + delta, 0.5), 1.5) * 10) / 10
}
</script>

<template>
  <div v-if="currentResume" class="editor-wrap">
    <!-- 简历切换条：下拉切换 + 新建 / 复制 / 删除 + 版本 / 分享 -->
    <div class="editor-resume-bar card">
      <div class="resume-bar-left">
        <span class="toolbar-label muted">当前简历</span>
        <el-select
          :model-value="currentResume.id"
          size="small"
          style="width: 240px"
          @change="switchResume"
        >
          <el-option
            v-for="item in myResumes"
            :key="item.id"
            :label="item.title || '未命名简历'"
            :value="item.id"
          />
        </el-select>
        <el-button size="small" @click="handleNewResume">新建</el-button>
        <el-button size="small" @click="handleCopyResume">复制</el-button>
        <el-button size="small" type="danger" plain @click="handleDeleteResume">删除</el-button>
      </div>
      <div class="resume-bar-right">
        <span class="resume-state-tag" :class="isDraft ? 'tag-draft' : 'tag-published'">{{ isDraft ? '草稿' : '正式简历' }}</span>
        <el-button v-if="isDraft" size="small" type="success" plain @click="handlePublish">发布</el-button>
        <el-button size="small" @click="openVersions">版本历史</el-button>
        <el-button size="small" @click="openShare">分享</el-button>
      </div>
    </div>

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

        <template v-if="visualSelected">
          <el-button size="small" type="primary" plain @click="visualEditorVisible = true">编辑图表数据</el-button>
          <span class="toolbar-label muted">双击图表也可编辑</span>
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
        <button class="tool-button" title="缩小 (Ctrl+减号)" @click="zoomBy(-0.1)">−</button>
        <span class="zoom-value">{{ Math.round(zoom * 100) }}%</span>
        <button class="tool-button" title="放大 (Ctrl+加号)" @click="zoomBy(0.1)">＋</button>
      </div>
      <span class="save-status muted">
        {{ saveState === 'saving' ? '保存中…' : saveState === 'dirty' ? '有未保存改动' : '已自动保存' }}
      </span>
      <el-button size="small" title="保存草稿 (Ctrl+S)" @click="handleSave(false)">保存</el-button>
      <el-dropdown trigger="click" @command="handleExport">
        <el-button size="small" type="primary" :loading="exporting" title="导出简历">
          导出 <span class="export-caret">▾</span>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="pdf">导出 PDF（打印）</el-dropdown-item>
            <el-dropdown-item command="png">导出 PNG 图片</el-dropdown-item>
            <el-dropdown-item command="word">导出 Word 文档</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <div class="editor-body">
      <!-- 左侧素材面板：组件 / 模板 / AI 三个标签，独立滚动，便于拖拽到画布 -->
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
              :class="{ vip: isVipComponent(item), locked: lockedForUser(item) }"
              :draggable="!lockedForUser(item)"
              @dragstart="onLibraryDragStart($event, item)"
              @click="addComponent(item)"
            >
              <div class="library-card-icon">{{ item.label.slice(0, 1) }}</div>
              <div class="library-card-label">{{ item.label }}</div>
              <span v-if="isVipComponent(item)" class="library-vip-mark">会员</span>
              <span v-if="lockedForUser(item)" class="library-lock">🔒</span>
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
                  :class="{ vip: isVipComponent({ ...item, groupKey: group.key }), locked: lockedForUser({ ...item, groupKey: group.key }) }"
                  :draggable="!lockedForUser({ ...item, groupKey: group.key })"
                  @dragstart="onLibraryDragStart($event, { ...item, groupKey: group.key, groupLabel: group.label })"
                  @click="addComponent({ ...item, groupKey: group.key, groupLabel: group.label })"
                >
                  <div class="library-card-icon">{{ item.label.slice(0, 1) }}</div>
                  <div class="library-card-label">{{ item.label }}</div>
                  <span v-if="isVipComponent({ ...item, groupKey: group.key })" class="library-vip-mark">会员</span>
                  <span v-if="lockedForUser({ ...item, groupKey: group.key })" class="library-lock">🔒</span>
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

        <!-- AI 优化：润色 / 岗位适配 / 中英翻译，结果可替换到选中组件 -->
        <div v-if="activeTab === 'ai'">
          <p class="muted panel-hint">选中组件则优化该组件，否则优化全文</p>
          <div class="ai-action-row">
            <el-button type="primary" size="small" :loading="aiLoading" @click="handleAi('POLISH')">AI 润色</el-button>
            <el-button size="small" :loading="aiLoading" @click="handleAi('TRANSLATE')">中英翻译</el-button>
          </div>

          <div class="ai-jd-block">
            <span class="toolbar-label muted">岗位 JD（用于匹配度分析）</span>
            <el-input
              v-model="aiJobDescription"
              type="textarea"
              :rows="3"
              placeholder="粘贴目标岗位 JD，留空则使用简历的目标岗位"
              size="small"
            />
            <el-button class="full-button" size="small" :loading="aiLoading" @click="handleAi('JOB_MATCH')">分析 JD 匹配度</el-button>
          </div>

          <div v-if="aiScore !== null" class="ai-score-card">
            <span class="ai-score-label">匹配度 / 评分</span>
            <strong class="ai-score-value">{{ aiScore }}</strong>
          </div>

          <ul v-if="aiSuggestions.length" class="ai-suggestion-list">
            <li v-for="(tip, idx) in aiSuggestions" :key="idx">{{ tip }}</li>
          </ul>

          <el-button v-if="aiResult && selectedComponent" class="full-button" size="small" @click="applyAiResult">替换选中组件内容</el-button>
          <div class="ai-result">{{ aiResult || '选择上方任一 AI 能力，结果会显示在这里。' }}</div>
        </div>
      </aside>

      <!-- 右侧画布：直接铺满展示整页 A4，内容超出自动向下增页 -->
      <DragResumeCanvas
        :components="currentResume.components"
        :page-style="resumePageStyle"
        :selected-id="selectedId"
        :zoom="zoom"
        @select="handleSelect"
        @change="markDirty"
        @add="addComponent"
        @edit-visual="openVisualEditor"
      />
    </div>
  </div>

  <MemberUpgradeDialog
    v-model:visible="upgradeVisible"
    :packages="packages"
    :payment-enabled="systemConfig.paymentEnabled !== false"
    :shop-url="systemConfig.shopUrl"
  />

  <!-- 会员图表组件数据编辑抽屉 -->
  <VisualEditor v-model:visible="visualEditorVisible" :component="selectedComponent" />

  <!-- 版本历史抽屉 -->
  <el-drawer v-model="versionDrawerVisible" title="版本历史" size="380px">
    <div v-loading="versionLoading">
      <p class="muted panel-hint">每次保存会自动生成快照，最多保留最近 20 条。</p>
      <div v-if="versions.length" class="version-list">
        <div v-for="version in versions" :key="version.id" class="version-item">
          <div class="version-item-main">
            <span class="version-item-title">{{ version.title || '未命名' }}</span>
            <span class="version-item-time">{{ String(version.createTime).replace('T', ' ').slice(0, 19) }}</span>
          </div>
          <el-button size="small" @click="handleRestoreVersion(version)">回滚</el-button>
        </div>
      </div>
      <el-empty v-else description="暂无历史版本" />
    </div>
  </el-drawer>

  <!-- 分享弹窗 -->
  <el-dialog v-model="shareVisible" title="分享简历" width="460px">
    <div v-loading="shareLoading">
      <p class="muted">任何人都可以通过以下只读链接查看这份简历：</p>
      <div class="share-link-row">
        <el-input :model-value="shareLink" readonly />
        <el-button type="primary" @click="copyShareLink">复制</el-button>
      </div>
      <div class="share-stat">
        <span>累计浏览</span>
        <strong>{{ shareInfo?.viewCount ?? 0 }}</strong>
      </div>
    </div>
  </el-dialog>
</template>
