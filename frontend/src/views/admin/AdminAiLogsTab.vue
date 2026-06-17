<!--
  后台 AI 调用日志 Tab
  功能：后端分页查看用户 AI 功能调用记录（纯 DB 存储，逐行写入）
-->
<script setup>
import { onMounted, ref } from 'vue'
import { listAiCallLogs } from '../../api/admin'

const logs = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const refresh = async () => {
  loading.value = true
  try {
    const res = await listAiCallLogs({ page: page.value, size: pageSize.value })
    logs.value = res?.records ?? []
    total.value = res?.total ?? 0
  } finally {
    loading.value = false
  }
}

const handlePageChange = (p) => {
  page.value = p
  refresh()
}

const handleSizeChange = (s) => {
  pageSize.value = s
  page.value = 1
  refresh()
}

onMounted(refresh)

const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')
const featureLabel = (t) => ({
  POLISH: '内容润色', SCORE: '简历评分', JOB_MATCH: '岗位适配', TRANSLATE: '中英翻译', GRAMMAR: '语法检查'
}[t] || t || '-')
</script>

<template>
  <section class="admin-panel-card card">
    <div class="admin-section-toolbar">
      <div>
        <h3>AI 调用日志</h3>
        <p>逐行记录用户 AI 功能调用（纯数据库存储，后端分页），最新在前。</p>
      </div>
      <el-button @click="refresh">刷新</el-button>
    </div>

    <el-table :data="logs" stripe v-loading="loading">
      <template #empty>
        <el-empty description="暂无 AI 调用记录" />
      </template>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column prop="username" label="账号" min-width="120" show-overflow-tooltip />
      <el-table-column label="功能" width="120">
        <template #default="{ row }">{{ featureLabel(row.featureType) }}</template>
      </el-table-column>
      <el-table-column label="会员等级" width="120">
        <template #default="{ row }">{{ row.vipLevel || '免费版' }}</template>
      </el-table-column>
      <el-table-column prop="quotaCost" label="消耗额度" width="100" align="center" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ row.status || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" min-width="170">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
    </el-table>

    <div class="admin-pagination">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :current-page="page"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </section>
</template>

<style scoped>
.admin-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
