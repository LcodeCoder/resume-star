<!--
  后台社区管理
  功能：审核、删除用户投稿的案例和文章
-->
<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api/request'

const activeTab = ref('cases')
const cases = ref([])
const articles = ref([])

onMounted(loadData)

async function loadData() {
  const [caseList, articleList] = await Promise.all([
    request.get('/community/cases'),
    request.get('/community/articles')
  ])
  cases.value = caseList || []
  articles.value = articleList || []
}

const approveCase = async (id) => {
  await request.put(`/community/cases/${id}/approve`)
  ElMessage.success('已审核通过')
  await loadData()
}

const deleteCase = async (id) => {
  await ElMessageBox.confirm('确定删除此案例吗？', '删除确认', { type: 'warning' })
  await request.delete(`/community/cases/${id}`)
  ElMessage.success('已删除')
  await loadData()
}

const approveArticle = async (id) => {
  await request.put(`/community/articles/${id}/approve`)
  ElMessage.success('已审核通过')
  await loadData()
}

const deleteArticle = async (id) => {
  await ElMessageBox.confirm('确定删除此文章吗？', '删除确认', { type: 'warning' })
  await request.delete(`/community/articles/${id}`)
  ElMessage.success('已删除')
  await loadData()
}
</script>

<template>
  <div class="admin-community">
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
      </el-tab-pane>

      <el-tab-pane label="优化技巧" name="articles">
        <el-table :data="articles" stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column prop="author" label="作者" width="120" />
          <el-table-column prop="category" label="分类" width="120" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.featured ? 'success' : 'info'">
                {{ row.featured ? '已审核' : '待审核' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览" width="80" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button v-if="!row.featured" type="success" size="small" @click="approveArticle(row.id)">审核通过</el-button>
              <el-button type="danger" size="small" @click="deleteArticle(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.admin-community {
  padding: 20px;
}
</style>
