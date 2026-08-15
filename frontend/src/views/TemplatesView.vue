<!--
  模板库页面
  功能：按分类筛选简历模板，卡片渲染模板真实缩略图（迷你 A4 预览），
        悬停浮出"使用此模板"按钮，一键套用后跳转编辑器；会员模板展示标识（不拦截）
  设计参考：Canva 简历模板页——胶囊分类筛选 + 缩略图卡片网格
-->
<script setup>
import { onMounted, onBeforeUnmount, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { applyTemplate } from '../api/resume'
import { listTemplateCategories, listTemplates, toggleTemplateFavorite, incrementTemplateView } from '../api/template'
import { listMemberPackages } from '../api/member'
import { getUserSystemConfig } from '../api/user'
import { useUserStore } from '../store/user'
import TemplatePreview from '../components/template-preview/TemplatePreview.vue'
import MemberUpgradeDialog from '../components/member-tip/MemberUpgradeDialog.vue'

const router = useRouter()
const userStore = useUserStore()
const categories = ref([])
const templates = ref([])
const activeCategory = ref('')
const activeStyle = ref('')
const loading = ref(true)
const packages = ref([])
const visible = ref(false)
const systemConfig = ref({ paymentEnabled: false, mockPaymentEnabled: true })
const styleMenuOpen = ref(false)
const styleFilterRef = ref(null)

const activeStyleLabel = computed(() => activeStyle.value || '全部风格')

const pickStyle = (tag) => {
  activeStyle.value = tag
  styleMenuOpen.value = false
}

const onDocClick = (event) => {
  if (!styleFilterRef.value?.contains(event.target)) styleMenuOpen.value = false
}

/** 兼容历史模板数据：补齐简历页面级样式字段 */
const ensureResumeStyle = (resume) => ({
  ...resume,
  style: {
    background: '#ffffff',
    ...(resume?.style || {})
  }
})

const loadTemplates = async () => {
  loading.value = true
  try {
    templates.value = (await listTemplates({
      categoryCode: activeCategory.value,
      userId: userStore.profile?.id || 1
    })).map(ensureResumeStyle)
    // 当前风格在新分类结果中不存在时重置，避免筛到空列表
    if (activeStyle.value && !templates.value.some((t) => t.styleTag === activeStyle.value)) {
      activeStyle.value = ''
    }
  } finally {
    loading.value = false
  }
}

/** 当前结果集中出现的风格标签（去重），作为风格筛选项 */
const styleTags = computed(() => {
  const seen = []
  for (const t of templates.value) {
    if (t.styleTag && !seen.includes(t.styleTag)) seen.push(t.styleTag)
  }
  return seen
})

/** 叠加风格筛选后的可见模板 */
const visibleTemplates = computed(() =>
  activeStyle.value ? templates.value.filter((t) => t.styleTag === activeStyle.value) : templates.value
)

/** 切换分类并刷新列表（重置风格筛选） */
const switchCategory = async (code) => {
  activeCategory.value = code
  activeStyle.value = ''
  await loadTemplates()
}

onMounted(async () => {
  document.addEventListener('click', onDocClick)
  categories.value = await listTemplateCategories()
  await userStore.loadProfile()
  const [packageList, config] = await Promise.all([
    listMemberPackages(),
    getUserSystemConfig()
  ])
  packages.value = packageList
  systemConfig.value = config || systemConfig.value
  await loadTemplates()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
})

const isVipUser = () => !!userStore.profile?.vipLevel

// 模板预览弹窗
const previewVisible = ref(false)
const currentTemplate = ref(null)

/**
 * 查看模板详情（弹窗预览）
 */
const viewTemplate = async (template) => {
  currentTemplate.value = template
  previewVisible.value = true
  // 增加浏览量
  if (template.id) {
    try {
      await incrementTemplateView(template.id)
      template.viewCount = (template.viewCount || 0) + 1
    } catch (e) {
      console.error('增加浏览量失败', e)
    }
  }
}

/**
 * 套用模板
 * 作用：跳转到编辑器并传递模板ID，编辑器加载模板但不保存，等用户手动保存
 */
const useTemplate = async (template) => {
  if (template.vipTemplate && !isVipUser()) {
    visible.value = true
    return
  }
  router.push(`/editor?useTemplate=${template.id}`)
}

/**
 * 收藏 / 取消收藏模板
 * 作用：切换收藏状态并就地更新卡片上的收藏标识与收藏数，无需整列表刷新
 */
const toggleFavorite = async (template) => {
  // 未登录：只提示，不发请求、不跳转，避免整页错误页打断体验
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再收藏模板')
    return
  }
  try {
    const favorited = await toggleTemplateFavorite(template.id)
    template.favorited = favorited
    template.favoriteCount = Math.max(0, (template.favoriteCount || 0) + (favorited ? 1 : -1))
    ElMessage.success(favorited ? '已加入收藏' : '已取消收藏')
  } catch (e) {
    // 失败信息已由请求拦截器统一提示，这里吞掉即可，避免冒泡到 ErrorBoundary
    console.error('收藏操作失败', e)
  }
}

</script>

<template>
  <section class="template-index-header">
    <div class="template-index-title">
      <span>LAYOUT OBSERVATORY</span>
      <h1>模板星库</h1>
      <p>先看内容如何被组织，再选择外观。套用模板不会覆盖已填写的经历。</p>
    </div>
    <div class="template-toolbar">
      <nav class="cat-tabs" role="tablist" aria-label="行业分类">
        <button
          type="button"
          role="tab"
          class="cat-tab"
          :class="{ active: activeCategory === '' }"
          :aria-selected="activeCategory === ''"
          @click="switchCategory('')"
        >全部</button>
        <button
          v-for="item in categories"
          :key="item.id"
          type="button"
          role="tab"
          class="cat-tab"
          :class="{ active: activeCategory === item.code }"
          :aria-selected="activeCategory === item.code"
          @click="switchCategory(item.code)"
        >
          {{ item.name }}
          <small v-if="item.count != null">{{ item.count }}</small>
        </button>
      </nav>
      <div v-if="styleTags.length > 1" ref="styleFilterRef" class="style-filter" @keydown.escape="styleMenuOpen = false">
        <button
          type="button"
          class="style-trigger"
          :class="{ open: styleMenuOpen }"
          aria-haspopup="listbox"
          :aria-expanded="styleMenuOpen"
          aria-label="按风格筛选"
          @click.stop="styleMenuOpen = !styleMenuOpen"
        >
          <span class="style-kicker">风格</span>
          <span class="style-value">{{ activeStyleLabel }}</span>
          <i class="style-caret" aria-hidden="true"></i>
        </button>
        <div v-if="styleMenuOpen" class="style-menu" role="listbox">
          <button
            type="button"
            role="option"
            class="style-option"
            :class="{ active: activeStyle === '' }"
            :aria-selected="activeStyle === ''"
            @click="pickStyle('')"
          >全部风格</button>
          <button
            v-for="tag in styleTags"
            :key="tag"
            type="button"
            role="option"
            class="style-option"
            :class="{ active: activeStyle === tag }"
            :aria-selected="activeStyle === tag"
            @click="pickStyle(tag)"
          >{{ tag }}</button>
        </div>
      </div>
    </div>
  </section>

  <!-- 加载中：骨架屏占位，避免空白/闪烁 -->
  <section v-if="loading" class="template-grid">
    <article v-for="n in 8" :key="'sk-' + n" class="template-card card tpl-skeleton">
      <div class="tpl-cover sk-block"></div>
      <div class="template-meta">
        <div class="sk-line sk-line-lg"></div>
        <div class="sk-line sk-line-sm"></div>
      </div>
    </article>
  </section>

  <section v-else-if="visibleTemplates.length" class="template-grid">
    <article v-for="item in visibleTemplates" :key="item.id" class="template-card card">
      <!-- 模板缩略图：按组件数据等比渲染，悬停浮出套用按钮 -->
      <div class="tpl-cover" :class="{ locked: item.vipTemplate && !isVipUser() }" @click="viewTemplate(item)">
        <TemplatePreview :components="item.components" :page-style="item.style" size="fill" />
        <!-- 收藏按钮：右上角 -->
        <button
          class="tpl-fav-btn"
          :class="{ active: item.favorited }"
          :title="item.favorited ? '取消收藏' : '收藏模板'"
          @click.stop="toggleFavorite(item)"
        >{{ item.favorited ? '♥' : '♡' }}</button>
        <!-- 会员模板标识：优雅的角标 -->
        <span v-if="item.vipTemplate && !isVipUser()" class="tpl-vip-badge">
          <svg viewBox="0 0 24 24" width="12" height="12" fill="currentColor">
            <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"/>
          </svg>
          VIP
        </span>
        <div class="tpl-overlay">
          <el-button type="primary" @click.stop="useTemplate(item)">
            {{ item.vipTemplate && !isVipUser() ? '升级解锁' : '使用此模板' }}
          </el-button>
        </div>
      </div>
      <div class="template-meta">
        <h3>{{ item.name }} <span v-if="item.vipTemplate" class="vip-badge-small">VIP</span></h3>
        <p class="template-stats">
          <span>👁 {{ item.viewCount || 0 }}</span>
          <span>♥ {{ item.favoriteCount || 0 }}</span>
          <span v-if="item.styleTag">{{ item.styleTag }}</span>
        </p>
      </div>
    </article>
  </section>

  <el-empty v-else description="该筛选条件下暂无模板" />

  <!-- 模板详情弹窗 -->
  <el-dialog
    v-model="previewVisible"
    :title="currentTemplate?.name"
    width="920px"
    top="5vh"
    destroy-on-close
    class="template-preview-dialog"
  >
    <div v-if="currentTemplate" class="template-detail">
      <div class="template-detail-info">
        <div class="template-detail-tags">
          <el-tag v-if="currentTemplate.vipTemplate" type="warning" effect="plain">会员专属</el-tag>
          <el-tag v-if="currentTemplate.styleTag" effect="plain">{{ currentTemplate.styleTag }}</el-tag>
        </div>
        <div class="template-detail-stats">
          <span><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg> {{ currentTemplate.viewCount || 0 }} 浏览</span>
          <span><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg> {{ currentTemplate.favoriteCount || 0 }} 收藏</span>
        </div>
      </div>
      <div class="template-detail-preview">
        <TemplatePreview
          v-if="currentTemplate"
          :components="currentTemplate.components"
          :page-style="currentTemplate.style"
          size="large"
        />
      </div>
      <div class="template-detail-actions">
        <el-button @click="toggleFavorite(currentTemplate)">
          {{ currentTemplate.favorited ? '取消收藏' : '收藏模板' }}
        </el-button>
        <el-button type="primary" @click="useTemplate(currentTemplate); previewVisible = false">
          {{ currentTemplate.vipTemplate && !isVipUser() ? '升级会员解锁' : '使用此模板' }}
        </el-button>
      </div>
    </div>
  </el-dialog>

  <MemberUpgradeDialog
    v-model:visible="visible"
    :packages="packages"
    :payment-enabled="systemConfig.paymentEnabled !== false"
    :shop-url="systemConfig.shopUrl"
  />
</template>

<style scoped>
/*
  样式说明：与全站 iOS 浅色主题统一——白底卡片、细边框、克制阴影、靛蓝强调色 #5b5bd6。
  .page-header / .chip / .tpl-preview 复用全局样式，这里只补充本页特有的卡片与弹窗细节。
*/

/* 页面头部：留出与卡片网格的间距 */
.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 22px;
  font-weight: 600;
}

.page-header p {
  margin: 6px 0 0;
  font-size: 14px;
  color: var(--color-text-secondary);
}

/* 模板网格 */
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.template-card {
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 封面即 A4：预览铺满，不再在大框里缩一张小纸 */
.tpl-cover {
  position: relative;
  aspect-ratio: 210 / 297;
  display: block;
  padding: 0;
  background: oklch(1 0 0);
  overflow: hidden;
  cursor: pointer;
}

.tpl-cover.locked .tpl-preview {
  filter: grayscale(0.4);
  opacity: 0.85;
}

/* 收藏按钮 */
.tpl-fav-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 26px;
  height: 26px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  color: #c7c7cc;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease, transform 0.15s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  z-index: 2;
}

.tpl-fav-btn:hover {
  background: #ffffff;
  transform: scale(1.1);
}

.tpl-fav-btn.active {
  color: #ff4d4f;
}

/* VIP 角标：克制的金色描边胶囊 */
.tpl-vip-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 3px 8px;
  background: rgba(255, 255, 255, 0.92);
  color: #b8860b;
  border: 1px solid rgba(184, 134, 11, 0.3);
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 3px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  z-index: 2;
  letter-spacing: 0.3px;
}

/* 悬停遮罩：靛蓝半透明 + 套用按钮 */
.tpl-overlay {
  position: absolute;
  inset: 0;
  background: rgba(29, 29, 31, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.15s ease;
  backdrop-filter: blur(2px);
}

.tpl-cover:hover .tpl-overlay {
  opacity: 1;
}

.tpl-overlay .el-button {
  border-radius: var(--radius-sm);
  font-weight: 500;
  padding: 6px 14px;
}

/* 模板元信息 */
.template-meta {
  padding: 10px 12px 12px;
}

.template-meta h3 {
  margin: 0 0 4px;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text);
}

.vip-badge-small {
  display: inline-block;
  padding: 1px 7px;
  background: rgba(184, 134, 11, 0.1);
  color: #b8860b;
  border: 1px solid rgba(184, 134, 11, 0.25);
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.template-stats {
  margin: 0;
  font-size: 11px;
  color: var(--color-text-muted);
  display: flex;
  gap: 10px;
}

.template-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 模板详情弹窗 */
.template-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.template-detail-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.template-detail-tags {
  display: flex;
  gap: 8px;
}

.template-detail-stats {
  display: flex;
  gap: 18px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.template-detail-stats span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.template-detail-stats svg {
  opacity: 0.55;
}

.template-detail-preview {
  display: flex;
  justify-content: center;
  background: var(--surface-2);
  padding: 14px;
  border-radius: var(--radius-md);
  overflow: hidden;
}

.template-detail-preview :deep(.tpl-preview-large) {
  width: min(100%, calc(52vh * 0.707));
  max-width: 420px;
}

.template-detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 14px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.template-detail-actions .el-button {
  padding: 10px 22px;
  font-weight: 500;
}

.cat-tab small {
  margin-left: 5px;
  color: var(--muted);
  font-size: 11px;
  font-weight: 650;
}
.cat-tab.active small {
  color: var(--accent);
}

/* 骨架屏：加载占位（reduced-motion 下由全局守卫停用动画） */
.tpl-skeleton {
  pointer-events: none;
}

.sk-block,
.sk-line {
  background: linear-gradient(90deg, #ececef 25%, #f5f5f7 37%, #ececef 63%);
  background-size: 400% 100%;
  animation: skShimmer 1.4s ease infinite;
}

.tpl-skeleton .tpl-cover.sk-block {
  aspect-ratio: 210 / 297;
}

.sk-line {
  height: 12px;
  border-radius: var(--radius-sm);
  margin-top: 10px;
}

.sk-line-lg {
  width: 68%;
}

.sk-line-sm {
  width: 40%;
}

@keyframes skShimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: 0 0;
  }
}

/* ===== 暗色模式：封面承托 / 收藏按钮 / 角标 / 分隔线 / 骨架屏 ===== */
:root[data-theme^='night'] .template-detail-preview {
  background: var(--space-deep);
}

:root[data-theme^='night'] .tpl-fav-btn {
  background: rgba(30, 40, 70, 0.92);
  border-color: rgba(255, 255, 255, 0.1);
}

:root[data-theme^='night'] .tpl-fav-btn:hover {
  background: #28335a;
}

:root[data-theme^='night'] .tpl-vip-badge {
  background: rgba(30, 40, 70, 0.9);
}

:root[data-theme^='night'] .template-detail-info,
:root[data-theme^='night'] .template-detail-actions {
  border-color: rgba(255, 255, 255, 0.08);
}

:root[data-theme^='night'] .sk-block,
:root[data-theme^='night'] .sk-line {
  background: linear-gradient(90deg, #16203a 25%, #1f2a48 37%, #16203a 63%);
  background-size: 400% 100%;
}

/* Orbit redesign: hierarchy-first archive, no office-style header card */
.template-index-header {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 22px;
  padding: 8px 0 0;
}
.template-index-title > span { color: var(--accent); font: 750 9px/1.4 ui-monospace, monospace; letter-spacing: .14em; }
.template-index-title h1 { margin: 9px 0 8px; font-size: 30px; letter-spacing: -.03em; }
.template-index-title p { max-width: 52ch; margin: 0; color: var(--ink-2); }

.template-toolbar {
  display: flex;
  align-items: stretch;
  gap: 16px;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}
.cat-tabs {
  display: flex;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}
.cat-tabs::-webkit-scrollbar { display: none; }
.cat-tab {
  flex: 0 0 auto;
  min-height: 38px;
  padding: 0 12px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--ink-2);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: color 160ms cubic-bezier(.16, 1, .3, 1), border-color 160ms cubic-bezier(.16, 1, .3, 1);
}
.cat-tab:hover { color: var(--ink); }
.cat-tab.active { color: var(--accent); border-bottom-color: var(--accent); }
.cat-tab:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: -2px;
}

.style-filter {
  position: relative;
  display: flex;
  align-items: center;
  flex-shrink: 0;
  padding: 0 0 0 12px;
  border-left: 1px solid var(--line);
}
.style-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 196px;
  height: 28px;
  padding: 0 8px 0 10px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  color: var(--ink);
  font: inherit;
  cursor: pointer;
  transition: border-color 160ms cubic-bezier(.16, 1, .3, 1), background 160ms cubic-bezier(.16, 1, .3, 1);
}
.style-trigger:hover,
.style-trigger.open {
  border-color: var(--accent);
  background: var(--surface);
}
.style-trigger:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.style-kicker {
  color: var(--muted);
  font-size: 11px;
  font-weight: 650;
}
.style-value {
  overflow: hidden;
  min-width: 0;
  color: var(--ink);
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.style-caret {
  width: 0;
  height: 0;
  margin-left: 2px;
  border-left: 3.5px solid transparent;
  border-right: 3.5px solid transparent;
  border-top: 4px solid var(--muted);
  transition: transform 160ms cubic-bezier(.16, 1, .3, 1);
}
.style-trigger.open .style-caret { transform: rotate(180deg); }
.style-menu {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  z-index: var(--z-dropdown);
  display: grid;
  min-width: 168px;
  max-height: 280px;
  overflow: auto;
  padding: 4px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface);
  box-shadow: var(--shadow);
}
.style-option {
  display: block;
  width: 100%;
  min-height: 30px;
  padding: 0 10px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--ink-2);
  font-size: 12px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
}
.style-option:hover { background: var(--surface-2); color: var(--ink); }
.style-option.active { background: var(--accent-soft); color: var(--accent); font-weight: 650; }

.template-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.template-card { border-radius: var(--radius-md); box-shadow: none; }
.template-card:first-child { grid-column: auto; }
.tpl-cover :deep(.tpl-preview-fill) {
  width: 100%;
  height: 100%;
}
.tpl-fav-btn { border-color: var(--line); background: var(--surface); color: var(--muted); box-shadow: none; }
.tpl-overlay { background: color-mix(in oklch, var(--space-deep), transparent 18%); backdrop-filter: none; }
.template-meta { border-top: 1px solid var(--line); }
.template-stats { color: var(--muted); }
@media (max-width: 1180px) {
  .template-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 860px) {
  .template-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 720px) {
  .template-toolbar { flex-direction: column; gap: 0; }
  .style-filter {
    justify-content: flex-start;
    padding: 8px 0;
    border-left: 0;
    border-top: 1px solid var(--line);
  }
  .style-menu { left: 0; right: auto; }
}
@media (max-width: 570px) {
  .template-grid { grid-template-columns: 1fr; }
  .template-card { display: grid; grid-template-columns: minmax(120px, .62fr) 1fr; }
  .template-card .tpl-cover { min-height: 200px; aspect-ratio: auto; }
  .template-card .template-meta {
    display: flex;
    justify-content: flex-end;
    flex-direction: column;
    border-top: 0;
    border-left: 1px solid var(--line);
  }
}
</style>
