<!--
  模板库页面
  功能：按分类筛选简历模板，卡片渲染模板真实缩略图（迷你 A4 预览），
        悬停浮出"使用此模板"按钮，一键套用后跳转编辑器；会员模板展示标识（不拦截）
  设计参考：Canva 简历模板页——胶囊分类筛选 + 缩略图卡片网格
-->
<script setup>
import { onMounted, ref } from 'vue'
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
const packages = ref([])
const visible = ref(false)
const systemConfig = ref({ paymentEnabled: false, mockPaymentEnabled: true })

/** 兼容历史模板数据：补齐简历页面级样式字段 */
const ensureResumeStyle = (resume) => ({
  ...resume,
  style: {
    background: '#ffffff',
    ...(resume?.style || {})
  }
})

const loadTemplates = async () => {
  templates.value = (await listTemplates({
    categoryCode: activeCategory.value,
    userId: userStore.profile?.id || 1
  })).map(ensureResumeStyle)
}

/** 切换分类并刷新列表 */
const switchCategory = async (code) => {
  activeCategory.value = code
  await loadTemplates()
}

onMounted(async () => {
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
  const favorited = await toggleTemplateFavorite(template.id, { userId: userStore.profile?.id || 1 })
  template.favorited = favorited
  template.favoriteCount = Math.max(0, (template.favoriteCount || 0) + (favorited ? 1 : -1))
  ElMessage.success(favorited ? '已加入收藏' : '已取消收藏')
}

</script>

<template>
  <section class="page-header card">
    <h2>简历模板库</h2>
    <p>挑选一套喜欢的模板，一键套用到编辑器继续创作。</p>
    <div class="chip-row">
      <button class="chip" :class="{ active: activeCategory === '' }" @click="switchCategory('')">全部</button>
      <button
        v-for="item in categories"
        :key="item.id"
        class="chip"
        :class="{ active: activeCategory === item.code }"
        @click="switchCategory(item.code)"
      >
        {{ item.name }} {{ item.count }}
      </button>
    </div>
  </section>

  <section class="template-grid">
    <article v-for="item in templates" :key="item.id" class="template-card card">
      <!-- 模板缩略图：按组件数据等比渲染，悬停浮出套用按钮 -->
      <div class="tpl-cover" :class="{ locked: item.vipTemplate && !isVipUser() }" @click="viewTemplate(item)">
        <TemplatePreview :components="item.components" :page-style="item.style" size="medium" />
        <!-- 小铃铛图标：点击查看详情 -->
        <button class="tpl-bell-btn" title="查看详情" @click.stop="viewTemplate(item)">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
            <path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2zm-2 1H8v-6c0-2.48 1.51-4.5 4-4.5s4 2.02 4 4.5v6z"/>
          </svg>
        </button>
        <!-- 收藏按钮：右上角 -->
        <button
          class="tpl-fav-btn"
          :class="{ active: item.favorited }"
          :title="item.favorited ? '取消收藏' : '收藏模板'"
          @click.stop="toggleFavorite(item)"
        >{{ item.favorited ? '♥' : '♡' }}</button>
        <!-- 会员模板标识 -->
        <span v-if="item.vipTemplate && !isVipUser()" class="tpl-lock-badge">🔒 会员</span>
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

  <!-- 模板详情弹窗 -->
  <el-dialog
    v-model="previewVisible"
    :title="currentTemplate?.name"
    width="1100px"
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
/* 页面头部 */
.page-header {
  padding: 2rem;
  margin-bottom: 2rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.3);
}

.page-header h2 {
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
  font-weight: 600;
}

.page-header p {
  margin: 0 0 1.5rem 0;
  opacity: 0.95;
  font-size: 1rem;
}

/* 分类筛选 */
.chip-row {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.chip {
  padding: 0.5rem 1.25rem;
  border: none;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
  backdrop-filter: blur(10px);
}

.chip:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

.chip.active {
  background: white;
  color: #667eea;
  font-weight: 500;
}

/* 模板网格 */
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 2rem;
  padding: 0 1rem;
}

.template-card {
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
  background: white;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.template-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
}

/* 模板封面 */
.tpl-cover {
  position: relative;
  aspect-ratio: 210 / 297;
  background: #f5f5f5;
  overflow: hidden;
  cursor: pointer;
}

.tpl-cover.locked {
  filter: grayscale(0.5);
}

/* 小铃铛按钮 */
.tpl-bell-btn {
  position: absolute;
  top: 12px;
  left: 12px;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.95);
  color: #667eea;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 2;
}

.tpl-bell-btn:hover {
  background: white;
  transform: scale(1.1) rotate(15deg);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

/* 收藏按钮 */
.tpl-fav-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.95);
  color: #ccc;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 2;
}

.tpl-fav-btn:hover {
  background: white;
  transform: scale(1.1);
}

.tpl-fav-btn.active {
  color: #ff4757;
  background: white;
}

/* 锁定标识 */
.tpl-lock-badge {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: 0.5rem 1rem;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  border-radius: 8px;
  font-size: 0.9rem;
  z-index: 2;
}

/* 悬停遮罩 */
.tpl-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.tpl-cover:hover .tpl-overlay {
  opacity: 1;
}

/* 模板元信息 */
.template-meta {
  padding: 1rem;
}

.template-meta h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.vip-badge-small {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 600;
}

.template-stats {
  margin: 0;
  font-size: 0.85rem;
  color: #999;
  display: flex;
  gap: 1rem;
}

.template-stats span {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

/* 模板详情弹窗 */
.template-detail {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.template-detail-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
}

.template-detail-tags {
  display: flex;
  gap: 0.5rem;
}

.template-detail-stats {
  display: flex;
  gap: 1.5rem;
  font-size: 0.9rem;
  color: #666;
}

.template-detail-stats span {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.template-detail-stats svg {
  opacity: 0.6;
}

.template-detail-preview {
  display: flex;
  justify-content: center;
  background: #f5f5f5;
  padding: 2rem;
  border-radius: 8px;
  max-height: 60vh;
  overflow-y: auto;
}

.template-detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #eee;
}
</style>
