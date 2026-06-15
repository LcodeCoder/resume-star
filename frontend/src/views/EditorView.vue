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
import { updateTemplate } from '../api/admin'
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

/**
 * 操作前登录校验：未登录时弹提示并拦截。
 * 页面允许匿名浏览/编辑，但保存、导出、AI、复制、删除、分享等需要账号的操作必须先登录。
 */
const requireLogin = () => {
  if (userStore.isLoggedIn) return true
  ElMessage.warning('请先登录后再操作')
  return false
}
const isAdminMode = ref(route.query.adminMode === 'true')
const editingTemplateId = ref(route.query.templateId ? Number(route.query.templateId) : null)
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
const shortcutVisible = ref(false)
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

/* ===== 撤销 / 重做历史栈 ===== */
/** 历史快照栈（每项为 components+style+title 的深拷贝 JSON） */
const undoStack = ref([])
/** 重做栈 */
const redoStack = ref([])
/** 正在应用撤销/重做时，跳过历史记录，避免循环入栈 */
let applyingHistory = false
/** 历史防抖：连续输入合并为一次快照 */
let historyTimer = null
/** 历史栈上限，避免无限增长 */
const HISTORY_LIMIT = 50

/** 组件剪贴板：存放被复制组件的深拷贝 JSON，供 Ctrl+V 粘贴 */
let componentClipboard = null

/** 生成当前简历内容快照（仅内容相关字段） */
const snapshot = () => {
  if (!currentResume.value) return null
  return JSON.stringify({
    title: currentResume.value.title,
    components: currentResume.value.components,
    style: currentResume.value.style
  })
}

/** 记录一次历史快照（防抖合并连续编辑） */
const pushHistory = () => {
  if (applyingHistory) return
  clearTimeout(historyTimer)
  historyTimer = setTimeout(() => {
    const snap = snapshot()
    if (snap == null) return
    const top = undoStack.value[undoStack.value.length - 1]
    if (snap === top) return
    undoStack.value.push(snap)
    if (undoStack.value.length > HISTORY_LIMIT) undoStack.value.shift()
    redoStack.value = []
  }, 350)
}

/** 应用一个快照到当前简历 */
const applySnapshot = (snap) => {
  if (!snap || !currentResume.value) return
  const data = JSON.parse(snap)
  applyingHistory = true
  currentResume.value.title = data.title
  currentResume.value.components = data.components
  currentResume.value.style = data.style
  selectedId.value = ''
  // 解除 applying 标记需等响应式更新与 watch 触发后
  setTimeout(() => { applyingHistory = false }, 0)
}

/** 撤销 */
const undo = () => {
  if (undoStack.value.length <= 1) {
    ElMessage.info('没有可撤销的操作')
    return
  }
  const current = undoStack.value.pop()
  redoStack.value.push(current)
  const prev = undoStack.value[undoStack.value.length - 1]
  applySnapshot(prev)
  markDirty()
}

/** 重做 */
const redo = () => {
  if (!redoStack.value.length) {
    ElMessage.info('没有可重做的操作')
    return
  }
  const snap = redoStack.value.pop()
  undoStack.value.push(snap)
  applySnapshot(snap)
  markDirty()
}

/** 重置历史栈（切换简历时调用），以传入简历为初始快照 */
const resetHistory = () => {
  undoStack.value = []
  redoStack.value = []
  // 延迟一拍，待 currentResume 就绪后写入初始快照
  setTimeout(() => {
    const snap = snapshot()
    if (snap != null) undoStack.value = [snap]
  }, 0)
}

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
const isVipUser = () => !!userStore.profile?.vipLevel

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

  // 管理员模式：加载模板数据
  if (isAdminMode.value && editingTemplateId.value) {
    const [templateList, vipConfig, config] = await Promise.all([
      listTemplates({ categoryCode: '' }),
      getTemplateVipConfig(),
      getUserSystemConfig()
    ])
    templates.value = templateList.map(ensureResumeStyle)
    const template = templates.value.find(t => t.id === editingTemplateId.value)
    if (template) {
      currentResume.value = ensureResumeStyle({ ...template, title: template.name })
      suppressAutosave = false
    }
    vipComponentGroups.value = vipConfig?.vipComponentGroups || []
    vipComponentKeys.value = vipConfig?.vipComponentKeys || []
    systemConfig.value = config || systemConfig.value
    document.addEventListener('keydown', onKeydown)
    return
  }

  // 普通用户模式（允许匿名浏览编辑器）：未登录则跳过「我的简历」等需登录接口，仅加载公开数据，以空白简历起步
  const [resumes, templateList, vipConfig, packageList, config] = await Promise.all([
    userStore.isLoggedIn ? listResumes({ userId: userStore.profile?.id }) : Promise.resolve([]),
    listTemplates({ categoryCode: '' }),
    getTemplateVipConfig(),
    listMemberPackages(),
    getUserSystemConfig()
  ])
  myResumes.value = (resumes || []).map(ensureResumeStyle)
  templates.value = templateList.map(ensureResumeStyle)
  vipComponentGroups.value = vipConfig?.vipComponentGroups || []
  vipComponentKeys.value = vipConfig?.vipComponentKeys || []
  packages.value = packageList
  systemConfig.value = config || systemConfig.value

  // 如果是从模板库跳转来的（useTemplate参数），加载模板但不保存
  const templateId = route.query.useTemplate ? Number(route.query.useTemplate) : null
  if (templateId) {
    const template = templates.value.find(t => t.id === templateId)
    if (template) {
      suppressAutosave = true
      selectedId.value = ''
      currentResume.value = ensureResumeStyle({ ...template, id: null, title: template.name, ownerId: userStore.profile?.id || 1, draft: true })
      saveState.value = 'unsaved'
      document.addEventListener('keydown', onKeydown)
      return
    }
  }

  // 支持 /editor?id=xxx 打开指定简历，否则默认第一份；如果没有任何简历则创建空白简历
  const wantId = route.query.id ? Number(route.query.id) : null
  let target = (wantId && myResumes.value.find((r) => r.id === wantId)) || myResumes.value[0]
  if (!target) {
    // 没有历史简历时创建空白简历（内存中）
    target = {
      id: null,
      ownerId: userStore.profile?.id || 1,
      title: '未命名简历',
      targetJob: '',
      templateId: null,
      draft: true,
      components: [],
      style: { background: '#ffffff', padding: 40, lineHeight: 1.6, fontSize: 14 },
      updateTime: null
    }
    suppressAutosave = true
    saveState.value = 'unsaved'
  }
  currentResume.value = ensureResumeStyle(target)
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
  // 禁用自动保存，只标记状态，等用户手动保存
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
    pushHistory()
    markDirty()
  },
  { deep: true }
)

// 切换简历（id 变化）时重置历史栈，避免跨简历误撤销
watch(
  () => currentResume.value?.id,
  () => resetHistory()
)

/**
 * 保存简历草稿
 * @param silent 是否静默保存（自动保存时不弹提示）
 */
const handleSave = async (silent = false) => {
  if (!currentResume.value) return
  if (!requireLogin()) return
  saveState.value = 'saving'
  const saved = await saveResume({
    id: currentResume.value.id,
    title: currentResume.value.title,
    targetJob: currentResume.value.targetJob,
    templateId: currentResume.value.templateId,
    draft: currentResume.value.draft === true,
    components: currentResume.value.components,
    style: currentResume.value.style,
    userId: userStore.profile?.id || 1
  })
  currentResume.value.id = saved.id
  syncResumeToList(saved)
  saveState.value = 'saved'
  if (!silent) ElMessage.success('保存成功')
}

/** 管理员保存模板 */
const handleSaveTemplate = async () => {
  if (!currentResume.value || !editingTemplateId.value) return
  saveState.value = 'saving'
  await updateTemplate(editingTemplateId.value, {
    name: currentResume.value.title,
    components: currentResume.value.components,
    style: currentResume.value.style
  })
  saveState.value = 'saved'
  ElMessage.success('模板已保存')
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
  // 如果当前是未保存的新简历，提示保存
  if (!currentResume.value.id && (saveState.value === 'dirty' || saveState.value === 'unsaved')) {
    await ElMessageBox.confirm('当前简历未保存，是否保存后再切换？', '提示', {
      confirmButtonText: '保存',
      cancelButtonText: '不保存',
      type: 'warning'
    }).then(async () => {
      await handleSave(true)
    }).catch(() => {
      // 用户选择不保存，直接切换
    })
  } else if (saveState.value !== 'saved') {
    await handleSave(true)
  }
  const list = await listResumes({ userId: userStore.profile?.id || 1 })
  myResumes.value = (list || []).map(ensureResumeStyle)
  const target = myResumes.value.find((r) => r.id === id)
  if (target) {
    suppressAutosave = true
    selectedId.value = ''
    currentResume.value = ensureResumeStyle(target)
  }
}

/** 新建空白简历并切换过去（仅内存，不存储） */
const handleNewResume = async () => {
  if (currentResume.value && saveState.value !== 'saved') await handleSave(true)
  const blank = {
    id: null,
    ownerId: userStore.profile?.id || 1,
    title: '未命名简历',
    targetJob: '',
    templateId: null,
    draft: true,
    components: [],
    style: { background: '#ffffff', padding: 40, lineHeight: 1.6, fontSize: 14 },
    updateTime: null
  }
  suppressAutosave = true
  selectedId.value = ''
  currentResume.value = ensureResumeStyle(blank)
  saveState.value = 'unsaved'
  ElMessage.success('已新建空白简历，点击保存按钮以存储')
}

/** 复制当前简历 */
const handleCopyResume = async () => {
  if (!currentResume.value) return
  if (!requireLogin()) return
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
  if (!requireLogin()) return
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
/** 当前简历是否为草稿 */
const isDraft = computed(() => currentResume.value?.draft === true)

/** 存为草稿：将正式简历标记为草稿 */
const saveDraft = async () => {
  if (!currentResume.value) return
  if (!requireLogin()) return
  currentResume.value.draft = true
  await handleSave(true)
  ElMessage.success('已存为草稿')
}

/** 发布：将草稿标记为正式简历 */
const publishCurrent = async () => {
  if (!currentResume.value) return
  if (!requireLogin()) return
  currentResume.value.draft = false
  await handleSave(true)
  ElMessage.success('已发布为正式简历')
}

/* ===== 版本历史 ===== */
const openVersions = async () => {
  if (!currentResume.value) return
  if (!requireLogin()) return
  // 未保存的新简历 id 为 null，需先落库拿到真实 id，避免请求 /resumes/null/versions 报错
  if (!currentResume.value.id) await handleSave(true)
  if (!currentResume.value.id) return
  versionDrawerVisible.value = true
  versionLoading.value = true
  try {
    versions.value = await listResumeVersions(currentResume.value.id) || []
  } finally {
    versionLoading.value = false
  }
}

const handleRestoreVersion = async (version) => {
  if (!requireLogin()) return
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
  if (!requireLogin()) return
  // 未保存的新简历 id 为 null，需先落库拿到真实 id，避免请求 /resumes/null/share 报错
  if (!currentResume.value.id || saveState.value !== 'saved') await handleSave(true)
  if (!currentResume.value.id) return
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
 * 删除当前选中组件（编辑器内不再二次确认，可用 Ctrl+Z 撤销）
 */
const removeSelected = () => {
  const components = currentResume.value?.components
  if (!components || !selectedId.value) return

  const index = components.findIndex((item) => item.id === selectedId.value)
  if (index < 0) return
  components.splice(index, 1)
  selectedId.value = ''
}

/**
 * 键盘删除：Delete / Backspace 移除选中组件（输入框、文本域聚焦时不触发）
 */
const onKeydown = (event) => {
  const tag = event.target?.tagName
  const isInput = tag === 'INPUT' || tag === 'TEXTAREA'
  const mod = event.ctrlKey || event.metaKey

  // Ctrl+S / Cmd+S: 保存
  if (mod && event.key === 's') {
    event.preventDefault()
    handleSave(false)
    return
  }

  // Ctrl+P / Cmd+P: 预览（导出 PDF）
  if (mod && event.key === 'p') {
    event.preventDefault()
    handleExport('pdf')
    return
  }

  // Ctrl+Shift+Z / Ctrl+Y: 重做；Ctrl+Z: 撤销（输入框内交给浏览器原生处理）
  if (mod && !isInput && (event.key === 'z' || event.key === 'Z')) {
    event.preventDefault()
    if (event.shiftKey) redo()
    else undo()
    return
  }
  if (mod && !isInput && (event.key === 'y' || event.key === 'Y')) {
    event.preventDefault()
    redo()
    return
  }

  // Ctrl+C: 复制选中组件；Ctrl+V: 粘贴（输入框内交给浏览器原生处理）
  if (mod && !isInput && (event.key === 'c' || event.key === 'C') && selectedId.value) {
    event.preventDefault()
    copySelected()
    return
  }
  if (mod && !isInput && (event.key === 'v' || event.key === 'V') && clipboard.value) {
    event.preventDefault()
    pasteClipboard()
    return
  }

  // Delete / Backspace: 删除选中组件（不在输入框内时）
  if (!isInput && (event.key === 'Delete' || event.key === 'Backspace') && selectedId.value) {
    event.preventDefault()
    removeSelected()
  }
}

/** 复制选中组件到内部剪贴板 */
const copySelected = () => {
  const component = selectedComponent.value
  if (!component) return
  clipboard.value = JSON.parse(JSON.stringify(component))
  ElMessage.success('已复制组件')
}

/** 粘贴内部剪贴板中的组件（错开 16px，分配新 ID） */
const pasteClipboard = () => {
  if (!clipboard.value || !currentResume.value?.components) return
  const copy = {
    ...JSON.parse(JSON.stringify(clipboard.value)),
    id: `c${Date.now()}`,
    x: (clipboard.value.x || 0) + 16,
    y: (clipboard.value.y || 0) + 16
  }
  currentResume.value.components.push(copy)
  handleSelect(copy.id)
  ElMessage.success('已粘贴组件')
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
// 垂直对齐：top/middle/bottom，配合 textAlign 把文字放到框内任意角（如右下角）
const setVAlign = (value) => {
  if (selectedComponent.value) selectedComponent.value.style.alignV = value
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
  if (!requireLogin()) return
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
    // 优先展示后端返回的具体原因（如额度用尽、未配置 API Key），否则兜底文案
    const msg = error?.response?.data?.message || error?.message || 'AI 请求失败，请稍后重试'
    ElMessage.error(msg)
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
  if (!requireLogin()) return
  selectedId.value = ''
  const userId = userStore.profile?.id || 1
  const resumeId = currentResume.value.id || 1
  const fileBase = (currentResume.value.title || '我的简历').replace(/[\\/:*?"<>|]/g, '_')
  if (format === 'pdf') {
    await recordExport({ userId, resumeId, exportType: 'PDF', highDefinition: true })
    printResume(fileBase)
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

/**
 * 打印导出 PDF：把简历纸张克隆到一个隔离的 iframe 里再打印，
 * 这样编辑器的布局高度、滚动容器都不会污染打印结果（避免凭空多出空白页），
 * 同时剥离编辑器专用元素（新增页按钮、选中芯片、缩放手柄、分页参考线）。
 */
const printResume = (fileBase) => {
  const source = document.querySelector('.resume-page')
  if (!source) return

  // 克隆纸张并清理编辑器专用节点
  const page = source.cloneNode(true)
  page.querySelectorAll('.add-page-btn, .block-chip, .resize-handle, .page-break')
    .forEach((el) => el.remove())
  // 去掉缩放/阴影/圆角，按真实像素尺寸输出
  page.style.transform = 'none'
  page.style.boxShadow = 'none'
  page.style.borderRadius = '0'
  page.style.margin = '0'

  // 纸张真实尺寸（含多页时的总高度）
  const width = source.offsetWidth || 794

  // 复制当前页面的样式表，保证简历在 iframe 内渲染一致
  const headStyles = Array.from(
    document.querySelectorAll('style, link[rel="stylesheet"]')
  ).map((node) => node.outerHTML).join('\n')

  const iframe = document.createElement('iframe')
  iframe.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0;visibility:hidden;'
  document.body.appendChild(iframe)

  const doc = iframe.contentWindow.document
  doc.open()
  doc.write(`<!DOCTYPE html><html><head><meta charset="utf-8"><title>${fileBase}</title>
${headStyles}
<style>
  /* 单页 A4 尺寸（794×1123px ≈ 96dpi A4）；内容更高时自动分页，不再凭空多出空白页 */
  @page { size: ${width}px 1123px; margin: 0; }
  html, body { margin: 0 !important; padding: 0 !important; background: #fff; }
  /* 强制输出背景色（色块、章节色带、头像底色） */
  * { -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; }
  .resume-page {
    position: static !important; margin: 0 !important;
    min-height: 0 !important; box-shadow: none !important;
    border-radius: 0 !important; transform: none !important;
  }
</style>
</head><body>${page.outerHTML}</body></html>`)
  doc.close()

  const cleanup = () => setTimeout(() => iframe.remove(), 500)
  const triggerPrint = () => {
    iframe.contentWindow.focus()
    iframe.contentWindow.print()
    cleanup()
  }
  // 等待 iframe 内字体/样式加载完成再打印，避免空白或错位
  if (doc.readyState === 'complete') {
    setTimeout(triggerPrint, 300)
  } else {
    iframe.onload = () => setTimeout(triggerPrint, 300)
  }
}

/** 缩放步进控制，范围 50% - 150% */
const zoomBy = (delta) => {
  zoom.value = Math.round(Math.min(Math.max(zoom.value + delta, 0.5), 1.5) * 10) / 10
}
</script>

<template>
  <div v-if="currentResume" class="editor-wrap">
    <!-- 移动端提示：编辑器是桌面体验，窄屏仅建议预览 -->
    <div class="editor-mobile-hint">
      📱 简历编辑器在桌面端体验最佳（拖拽排版需要大屏）。手机上可浏览与微调，建议用电脑完成排版与导出。
    </div>
    <!-- 简历切换条：下拉切换 + 新建 / 复制 / 删除 + 版本 / 分享 -->
    <div v-if="!isAdminMode" class="editor-resume-bar card">
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
        <el-button v-if="isDraft" size="small" type="success" plain @click="publishCurrent">发布为正式</el-button>
        <el-button v-else size="small" type="warning" plain @click="saveDraft">存为草稿</el-button>
        <el-button size="small" @click="openVersions">版本历史</el-button>
        <el-button size="small" @click="openShare">分享</el-button>
        <el-button size="small" title="键盘快捷键" @click="shortcutVisible = true">快捷键</el-button>
      </div>
    </div>

    <!-- 管理员模式：模板编辑标题栏 -->
    <div v-else class="editor-resume-bar card">
      <div class="resume-bar-left">
        <span class="toolbar-label muted">编辑模板</span>
        <span style="font-weight: 600; margin-left: 8px">{{ currentResume.title }}</span>
      </div>
      <div class="resume-bar-right">
        <el-button size="small" type="primary" @click="handleSaveTemplate">保存模板</el-button>
        <el-button size="small" @click="$router.push('/admin')">返回管理后台</el-button>
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
          <div class="tool-group">
            <button class="tool-button" :class="{ active: (selectedComponent.style.alignV || 'top') === 'top' }" title="顶部对齐" @click="setVAlign('top')">⤒</button>
            <button class="tool-button" :class="{ active: selectedComponent.style.alignV === 'middle' }" title="垂直居中" @click="setVAlign('middle')">↕</button>
            <button class="tool-button" :class="{ active: selectedComponent.style.alignV === 'bottom' }" title="底部对齐" @click="setVAlign('bottom')">⤓</button>
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
          <span class="toolbar-label muted">文字</span>
          <el-input
            v-model="selectedComponent.content"
            size="small"
            placeholder="留空仅显示图标"
            clearable
            style="width: 180px"
          />
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

      <div class="history-controls">
        <button class="tool-button" title="撤销 (Ctrl+Z)" :disabled="undoStack.length <= 1" @click="undo">↶</button>
        <button class="tool-button" title="重做 (Ctrl+Shift+Z)" :disabled="!redoStack.length" @click="redo">↷</button>
      </div>

      <div class="zoom-controls">
        <button class="tool-button" title="缩小" @click="zoomBy(-0.1)">−</button>
        <span class="zoom-value">{{ Math.round(zoom * 100) }}%</span>
        <button class="tool-button" title="放大" @click="zoomBy(0.1)">＋</button>
      </div>
      <span class="save-status muted">
        {{ saveState === 'saving' ? '保存中…' : saveState === 'dirty' || saveState === 'unsaved' ? '未保存' : '已保存' }}
      </span>
      <el-button size="small" type="primary" title="保存 (Ctrl+S)" @click="handleSave(false)">保存</el-button>
      <el-button v-if="!isDraft && currentResume?.id" size="small" type="warning" plain @click="saveDraft">存为草稿</el-button>
      <el-dropdown trigger="click" @command="handleExport">
        <el-button size="small" :loading="exporting" title="导出简历">
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
          <!-- AI 处理中：骨架屏占位，比单纯转圈更有进度感 -->
          <div v-if="aiLoading" class="ai-skeleton">
            <span class="ai-skeleton-line"></span>
            <span class="ai-skeleton-line"></span>
            <span class="ai-skeleton-line short"></span>
            <span class="ai-skeleton-tip">AI 正在优化，请稍候…</span>
          </div>
          <div v-else class="ai-result">{{ aiResult || '选择上方任一 AI 能力，结果会显示在这里。' }}</div>
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

  <!-- 键盘快捷键提示 -->
  <el-dialog v-model="shortcutVisible" title="键盘快捷键" width="420px">
    <ul class="shortcut-list">
      <li><span>保存</span><kbd>Ctrl / ⌘ + S</kbd></li>
      <li><span>导出 PDF</span><kbd>Ctrl / ⌘ + P</kbd></li>
      <li><span>撤销</span><kbd>Ctrl / ⌘ + Z</kbd></li>
      <li><span>重做</span><kbd>Ctrl / ⌘ + Shift + Z（或 Ctrl + Y）</kbd></li>
      <li><span>复制选中组件</span><kbd>Ctrl / ⌘ + C</kbd></li>
      <li><span>粘贴组件</span><kbd>Ctrl / ⌘ + V</kbd></li>
      <li><span>删除选中组件</span><kbd>Delete / Backspace</kbd></li>
      <li><span>编辑文字 / 上传</span><kbd>双击组件</kbd></li>
    </ul>
    <p class="shortcut-tip muted">提示：在文本输入框内时，撤销/复制等交给浏览器原生处理。</p>
  </el-dialog>
</template>
