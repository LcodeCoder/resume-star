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
              <span class="case-icon">📄</span>
            </div>
            <div class="case-info">
              <h3>{{ item.title }}</h3>
              <p class="case-desc">{{ item.description }}</p>
              <div class="case-meta">
                <span>👁 {{ item.viewCount }}</span>
                <span>❤️ {{ item.likeCount }}</span>
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
                <span>👁 {{ item.viewCount }}</span>
                <span>•</span>
                <span>{{ formatDate(item.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 文章详情弹窗 -->
    <el-dialog v-model="articleVisible" :title="currentArticle?.title" width="720px" top="5vh">
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
    <el-dialog v-model="caseVisible" :title="currentCase?.title" width="720px" top="5vh">
      <div v-if="currentCase" class="case-detail">
        <div class="case-detail-meta">
          <span>作者：{{ currentCase.authorName }}</span>
          <span>•</span>
          <span>👁 {{ currentCase.viewCount }}</span>
          <span>•</span>
          <span>❤️ {{ currentCase.likeCount }}</span>
        </div>
        <div class="case-desc">{{ currentCase.description }}</div>
        <div class="case-tags">
          <el-tag v-for="tag in (currentCase.tags || '').split(',')" :key="tag" size="small">{{ tag }}</el-tag>
        </div>
        <el-alert type="info" :closable="false" style="margin-top: 20px">
          案例的完整简历数据展示功能开发中，敬请期待
        </el-alert>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

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
})

const viewCase = async (item) => {
  const detail = await request.get(`/community/cases/${item.id}`)
  currentCase.value = detail
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
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.case-thumb {
  text-align: center;
  margin-bottom: 16px;
}

.case-icon {
  font-size: 48px;
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
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
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
  color: #409eff;
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
  color: #409eff;
  font-weight: 600;
}
</style>

.case-detail-meta {
  display: flex;
  gap: 8px;
  font-size: 14px;
  color: #909399;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.case-desc {
  font-size: 15px;
  line-height: 1.8;
  color: #606266;
  margin-bottom: 16px;
}

.case-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
