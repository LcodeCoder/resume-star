<template>
  <div class="community-page">
    <div class="community-header">
      <h1>简历社区</h1>
      <p>优秀案例、实用技巧，助你打造完美简历</p>
    </div>

    <el-tabs v-model="activeTab" class="community-tabs">
      <el-tab-pane label="优秀案例" name="cases">
        <div class="cases-grid">
          <div v-for="item in cases" :key="item.id" class="case-card" @click="viewCase(item)">
            <div class="case-thumb">
              <TemplatePreview
                v-if="item.fullResume && item.fullResume.components"
                :components="item.fullResume.components"
                :page-style="item.fullResume.style"
                size="medium"
              />
              <span v-else class="case-icon">📄</span>
            </div>
            <div class="case-info">
              <h3>{{ item.title }}</h3>
              <p class="case-desc">{{ item.description }}</p>
              <div class="case-meta">
                <span><el-icon><User /></el-icon> {{ item.viewCount }}</span>
                <span><el-icon><Pointer /></el-icon> {{ item.likeCount }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="优化技巧" name="articles">
        <div class="articles-list">
          <div v-for="item in articles" :key="item.id" class="article-card" @click="viewArticle(item)">
            <div class="article-main">
              <h3>{{ item.title }}</h3>
              <p class="article-summary">{{ item.summary }}</p>
              <div class="article-meta">
                <span class="article-author">{{ item.author }}</span>
                <span>•</span>
                <span><el-icon><User /></el-icon> {{ item.viewCount }}</span>
                <span>•</span>
                <span>{{ formatDate(item.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 文章详情弹窗 -->
    <el-dialog v-model="articleVisible" :title="currentArticle?.title" width="720px" top="5vh" :close-on-click-modal="true">
      <div v-if="currentArticle" class="article-detail">
        <div class="article-detail-meta">
          <span>{{ currentArticle.author }}</span>
          <span>•</span>
          <span>{{ formatDate(currentArticle.createTime) }}</span>
        </div>
        <div class="article-content" v-html="renderMarkdown(currentArticle.content)"></div>
      </div>
    </el-dialog>

    <!-- 案例详情弹窗 -->
    <el-dialog
      v-model="caseVisible"
      width="1100px"
      top="5vh"
      :close-on-click-modal="true"
      destroy-on-close
      :show-close="false"
      class="case-dialog"
    >
      <template #header>
        <div v-if="currentCase" class="case-hero">
          <div class="case-hero-icon"><el-icon><Document /></el-icon></div>
          <div class="case-hero-info">
            <h2 class="case-hero-title">{{ currentCase.title }}</h2>
            <div class="case-hero-meta">
              <span class="meta-item author"><el-icon><User /></el-icon>{{ currentCase.authorName }}</span>
              <span class="meta-item"><el-icon><View /></el-icon>{{ currentCase.viewCount }} 浏览</span>
              <span class="meta-item"><el-icon><Pointer /></el-icon>{{ currentCase.likeCount }} 点赞</span>
            </div>
          </div>
          <div class="case-hero-actions">
            <el-button round @click="likeCase(currentCase.id)">
              <el-icon><Pointer /></el-icon> 点赞
            </el-button>
            <el-button type="primary" round @click="borrowCase(currentCase)">
              <el-icon><MagicStick /></el-icon> 借鉴此简历
            </el-button>
          </div>
        </div>
      </template>

      <div v-if="currentCase" class="case-detail">
        <div v-if="currentCase.description" class="case-desc">{{ currentCase.description }}</div>
        <div v-if="currentCase.tags" class="case-tags">
          <el-tag
            v-for="tag in currentCase.tags.split(',').filter(t => t.trim())"
            :key="tag"
            effect="light"
            round
          >
            {{ tag.trim() }}
          </el-tag>
        </div>
        <div v-if="currentCase.fullResume" class="case-resume-preview">
          <h4 class="section-title">简历预览</h4>
          <div class="resume-frame">
            <TemplatePreview
              :components="currentCase.fullResume.components"
              :page-style="currentCase.fullResume.style"
              size="large"
            />
          </div>
        </div>
        <div v-else-if="currentCase.resumeData" class="case-resume-data">
          <h4 class="section-title">简历预览</h4>
          <div class="resume-preview">{{ extractResumeContent(currentCase.resumeData) }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import request from '../api/request'
import TemplatePreview from '../components/template-preview/TemplatePreview.vue'
import { Document, User, View, Pointer, MagicStick } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('cases')
const cases = ref([])
const articles = ref([])
const articleVisible = ref(false)
const currentArticle = ref(null)
const caseVisible = ref(false)
const currentCase = ref(null)

onMounted(async () => {
  const [caseList, articleList] = await Promise.all([
    request.get('/community/cases'),
    request.get('/community/articles')
  ])
  cases.value = caseList || []
  articles.value = articleList || []
  loadCasePreviews()
})

/**
 * 为案例卡片加载脱敏后的简历数据，渲染真实预览缩略图（与模板卡片一致）。
 * 逐个并发请求，单个失败仅该卡片回退到占位图标，不影响其他卡片。
 */
const loadCasePreviews = () => {
  cases.value.forEach(async (item) => {
    try {
      const resume = await request.get(`/community/cases/${item.id}/resume`)
      if (resume && resume.components) {
        item.fullResume = resume
      }
    } catch (e) {
      // 单个预览加载失败时保留 📄 占位
    }
  })
}

const viewCase = async (item) => {
  const detail = await request.get(`/community/cases/${item.id}`)
  currentCase.value = detail
  // 优先复用列表已预加载的脱敏简历，避免重复请求；未命中再单独加载
  if (item.fullResume) {
    currentCase.value.fullResume = item.fullResume
  } else {
    try {
      const resume = await request.get(`/community/cases/${item.id}/resume`)
      if (resume) {
        currentCase.value.fullResume = resume
      }
    } catch (e) {
      console.error('加载简历失败', e)
      currentCase.value.fullResume = null
    }
  }
  caseVisible.value = true
}

const viewArticle = async (item) => {
  const detail = await request.get(`/community/articles/${item.id}`)
  currentArticle.value = detail
  articleVisible.value = true
}

const formatDate = (date) => {
  if (!date) return ''
  return String(date).slice(0, 10)
}

const renderMarkdown = (content) => {
  if (!content) return ''
  return content
    .replace(/^# (.+)$/gm, '<h2>$1</h2>')
    .replace(/^## (.+)$/gm, '<h3>$1</h3>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

const extractResumeContent = (data) => {
  if (!data) return ''
  // 移除 resumeId: 前缀行
  return data.replace(/^resumeId:\d+\n/, '')
}

const likeCase = async (id) => {
  const userId = userStore.profile?.id || 1
  const result = await request.post(`/community/cases/${id}/like?userId=${userId}`)
  ElMessage.success(result.liked ? '点赞成功' : '已取消点赞')
  currentCase.value = { ...currentCase.value, likeCount: result.likeCount }
}

const borrowCase = async (caseItem) => {
  const isVip = !!userStore.vipLevel
  if (!isVip) {
    ElMessage.warning('借鉴简历功能需要开通会员')
    return
  }
  if (!caseItem.fullResume) {
    ElMessage.error('简历数据加载失败')
    return
  }
  // 复制简历数据到新简历
  const userId = userStore.profile?.id || 1
  const newResume = await request.post('/resumes', {
    userId,
    title: '借鉴 - ' + caseItem.title,
    templateId: caseItem.fullResume.templateId,
    components: caseItem.fullResume.components,
    style: caseItem.fullResume.style,
    draft: true
  })
  caseVisible.value = false
  router.push({ path: '/editor', query: { id: newResume.id } })
}
</script>

<style scoped>
.community-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.community-header {
  text-align: center;
  margin-bottom: 32px;
}

.community-header h1 {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 12px;
}

.community-header p {
  font-size: 16px;
  color: #909399;
  margin: 0;
}

.community-tabs {
  margin-top: 24px;
}

.cases-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.case-card {
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.case-card:hover {
  border-color: #5b5bd6;
  box-shadow: 0 4px 12px rgba(91, 91, 214, 0.15);
}

.case-thumb {
  position: relative;
  aspect-ratio: 210 / 297;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  margin-bottom: 16px;
  background: #f5f5f7;
  border-radius: 8px;
  overflow: hidden;
}

.case-thumb :deep(.tpl-preview) {
  width: 100%;
}

.case-icon {
  font-size: 48px;
  align-self: center;
}

.case-info h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px;
}

.case-desc {
  font-size: 14px;
  color: #606266;
  margin: 0 0 12px;
  line-height: 1.6;
}

.case-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #909399;
}

.articles-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-card {
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.article-card:hover {
  border-color: #5b5bd6;
  box-shadow: 0 4px 12px rgba(91, 91, 214, 0.15);
}

.article-main h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 12px;
}

.article-summary {
  font-size: 14px;
  color: #606266;
  margin: 0 0 12px;
  line-height: 1.6;
}

.article-meta {
  display: flex;
  gap: 8px;
  font-size: 13px;
  color: #909399;
}

.article-author {
  color: #5b5bd6;
  font-weight: 500;
}

.article-detail-meta {
  display: flex;
  gap: 8px;
  font-size: 14px;
  color: #909399;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.article-content {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
}

.article-content h2 {
  font-size: 20px;
  font-weight: 600;
  margin: 24px 0 16px;
}

.article-content h3 {
  font-size: 17px;
  font-weight: 600;
  margin: 20px 0 12px;
}

.article-content strong {
  color: #5b5bd6;
  font-weight: 600;
}

/* ===== 案例详情弹窗 ===== */
.case-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
}

.case-dialog :deep(.el-dialog__header) {
  padding: 0 !important;
  margin: 0;
  background: linear-gradient(135deg, #5b5bd6 0%, #6a5cff 100%);
  display: block;
  position: relative;
}

.case-dialog :deep(.el-dialog__body) {
  max-height: 72vh;
  overflow-y: auto;
}

.case-dialog :deep(.el-dialog__headerbtn) {
  top: 18px;
  right: 18px;
  z-index: 2;
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
}

.case-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: #fff;
  font-size: 20px;
}

/* 头部 hero 区 */
.case-hero {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 26px 60px 26px 28px;
  background: linear-gradient(135deg, #5b5bd6 0%, #6a5cff 100%);
  color: #fff;
  width: 100%;
  box-sizing: border-box;
}

.case-hero-icon {
  flex-shrink: 0;
  width: 60px;
  height: 60px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}

.case-hero-info {
  flex: 1;
  min-width: 0;
}

.case-hero-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 8px;
  color: #fff;
  line-height: 1.3;
}

.case-hero-meta {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  font-size: 13px;
  opacity: 0.92;
}

.case-hero-meta .meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.case-hero-meta .meta-item .el-icon {
  font-size: 15px;
}

.case-hero-actions :deep(.el-button) .el-icon {
  margin-right: 4px;
}

.case-hero-actions {
  flex-shrink: 0;
  display: flex;
  gap: 10px;
}

.case-hero-actions :deep(.el-button) {
  border: none;
}

/* 描述区 */
.case-desc {
  font-size: 15px;
  line-height: 1.8;
  color: #4a4f57;
  margin-bottom: 20px;
  padding: 16px 18px;
  background: #f5f7fa;
  border-radius: 10px;
  border-left: 4px solid #5b5bd6;
}

/* 标签区 */
.case-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

/* 简历预览区 */
.case-resume-preview,
.case-resume-data {
  margin-top: 8px;
  padding-top: 24px;
  border-top: 1px solid #ebeef5;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 18px;
  padding-left: 12px;
  border-left: 4px solid #5b5bd6;
}

.resume-frame {
  background: #f0f2f5;
  border-radius: 12px;
  padding: 24px;
  overflow-x: auto;
}

.resume-frame :deep(.tpl-preview) {
  width: 760px;
  max-width: 100%;
  margin: 0 auto;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.resume-preview {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

/* ===== 暗色模式：案例 / 文章卡片、正文、分隔线、承托底 ===== */
:root[data-theme^='night'] .community-header p,
:root[data-theme^='night'] .case-meta,
:root[data-theme^='night'] .article-meta,
:root[data-theme^='night'] .article-detail-meta {
  color: var(--color-text-secondary);
}

:root[data-theme^='night'] .case-desc,
:root[data-theme^='night'] .article-summary,
:root[data-theme^='night'] .article-content {
  color: var(--color-text-tertiary);
}

:root[data-theme^='night'] .article-content,
:root[data-theme^='night'] .section-title {
  color: var(--color-text);
}

:root[data-theme^='night'] .case-card,
:root[data-theme^='night'] .article-card {
  border-color: rgba(255, 255, 255, 0.1);
}

:root[data-theme^='night'] .case-card:hover,
:root[data-theme^='night'] .article-card:hover {
  border-color: rgba(124, 130, 245, 0.5);
}

:root[data-theme^='night'] .article-content strong,
:root[data-theme^='night'] .article-author {
  color: #aab0ff;
}

:root[data-theme^='night'] .article-detail-meta {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

:root[data-theme^='night'] .case-resume-preview,
:root[data-theme^='night'] .case-resume-data {
  border-top-color: rgba(255, 255, 255, 0.08);
}

:root[data-theme^='night'] .case-desc {
  background: var(--color-elevated);
}

:root[data-theme^='night'] .resume-frame {
  background: #0d1426;
}

:root[data-theme^='night'] .case-thumb {
  background: #0d1426;
}
</style>
