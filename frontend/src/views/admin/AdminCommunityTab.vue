<!--
  后台社区管理
  功能：审核、删除用户投稿的案例和文章
-->
<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api/request'
import { getSystemConfig, updateSystemConfig } from '../../api/systemConfig'

const activeTab = ref('cases')
const cases = ref([])
const articles = ref([])
const casePage = ref(1)
const caseTotal = ref(0)
const articlePage = ref(1)
const articleTotal = ref(0)
const PAGE_SIZE = 10
const configSaving = ref(false)
const communityConfig = ref({ communityApprovalRewardExportEnabled: false })

onMounted(loadData)

async function loadCases() {
  const res = await request.get('/community/admin/cases', { params: { page: casePage.value, size: PAGE_SIZE } })
  cases.value = res?.records ?? []
  caseTotal.value = res?.total ?? 0
}

async function loadArticles() {
  const res = await request.get('/community/admin/articles', { params: { page: articlePage.value, size: PAGE_SIZE } })
  articles.value = res?.records ?? []
  articleTotal.value = res?.total ?? 0
}

async function loadData() {
  // 后台列表用 admin 端点：返回全部（含待审核且超过 1 小时展示窗口的）投稿，后端分页
  await Promise.all([loadCases(), loadArticles()])
  communityConfig.value = await getSystemConfig() || communityConfig.value
}

const handleCasePageChange = (p) => {
  casePage.value = p
  loadCases()
}

const handleArticlePageChange = (p) => {
  articlePage.value = p
  loadArticles()
}

const saveCommunityConfig = async () => {
  configSaving.value = true
  try {
    await updateSystemConfig({ communityApprovalRewardExportEnabled: communityConfig.value.communityApprovalRewardExportEnabled })
    ElMessage.success('社区奖励配置已保存')
  } finally {
    configSaving.value = false
  }
}

const approveCase = async (id) => {
  await request.put(`/community/cases/${id}/approve`)
  ElMessage.success('已审核通过')
  await loadCases()
}

const deleteCase = async (id) => {
  await ElMessageBox.confirm('确定删除此案例吗？', '删除确认', { type: 'warning' })
  await request.delete(`/community/cases/${id}`)
  ElMessage.success('已删除')
  if (cases.value.length === 1 && casePage.value > 1) casePage.value -= 1
  await loadCases()
}

const approveArticle = async (id) => {
  await request.put(`/community/articles/${id}/approve`)
  ElMessage.success('已审核通过')
  await loadArticles()
}

const deleteArticle = async (id) => {
  await ElMessageBox.confirm('确定删除此文章吗？', '删除确认', { type: 'warning' })
  await request.delete(`/community/articles/${id}`)
  ElMessage.success('已删除')
  if (articles.value.length === 1 && articlePage.value > 1) articlePage.value -= 1
  await loadArticles()
}
</script>

<template>
  <div class="admin-community">
    <section class="community-config card">
      <div>
        <h3>审核奖励</h3>
        <p>开启后，用户提交的优秀案例或优化技巧首次审核通过时，自动增加 1 次导出简历次数。</p>
      </div>
      <div class="community-config-actions">
        <el-switch v-model="communityConfig.communityApprovalRewardExportEnabled" active-text="奖励导出次数" inactive-text="不奖励" />
        <el-button type="primary" :loading="configSaving" @click="saveCommunityConfig">保存</el-button>
      </div>
    </section>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="简历案例" name="cases">
        <el-table :data="cases" stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column prop="authorName" label="作者" width="120" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.featured ? 'success' : 'info'">
                {{ row.featured ? '已审核' : '待审核' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览" width="80" />
          <el-table-column prop="likeCount" label="点赞" width="80" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button v-if="!row.featured" type="success" size="small" @click="approveCase(row.id)">审核通过</el-button>
              <el-button type="danger" size="small" @click="deleteCase(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="community-pagination">
          <el-pagination
            v-if="caseTotal > PAGE_SIZE"
            background
            layout="total, prev, pager, next"
            :total="caseTotal"
            :current-page="casePage"
            :page-size="PAGE_SIZE"
            @current-change="handleCasePageChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="优化技巧" name="articles">
        <el-table :data="articles" stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column prop="author" label="作者" width="120" />
          <el-table-column prop="category" label="分类" width="120" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.published ? 'success' : 'info'">
                {{ row.published ? '已审核' : '待审核' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览" width="80" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button v-if="!row.published" type="success" size="small" @click="approveArticle(row.id)">审核通过</el-button>
              <el-button type="danger" size="small" @click="deleteArticle(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="community-pagination">
          <el-pagination
            v-if="articleTotal > PAGE_SIZE"
            background
            layout="total, prev, pager, next"
            :total="articleTotal"
            :current-page="articlePage"
            :page-size="PAGE_SIZE"
            @current-change="handleArticlePageChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.admin-community {
  padding: 20px;
}

.community-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.community-config {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 18px 20px;
}

.community-config h3 {
  margin: 0 0 6px;
}

.community-config p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.community-config-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .community-config,
  .community-config-actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
