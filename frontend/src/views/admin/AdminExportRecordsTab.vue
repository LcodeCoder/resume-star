<!--
  后台导出记录 Tab
  功能：后端分页查看用户 PDF/图片导出记录（纯 DB 存储，逐行写入）
-->
<script setup>
import { onMounted, ref } from 'vue'
import { listExportRecords } from '../../api/admin'

const records = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const refresh = async () => {
  loading.value = true
  try {
    const res = await listExportRecords({ page: page.value, size: pageSize.value })
    records.value = res?.records ?? []
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
</script>

<template>
  <section class="admin-panel-card card">
    <div class="admin-section-toolbar">
      <div>
        <h3>导出记录</h3>
        <p>逐行记录用户简历导出（纯数据库存储，后端分页），最新在前。</p>
      </div>
      <el-button @click="refresh">刷新</el-button>
    </div>

    <el-table :data="records" stripe v-loading="loading">
      <template #empty>
        <el-empty description="暂无导出记录" />
      </template>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column prop="username" label="账号" min-width="120" show-overflow-tooltip />
      <el-table-column prop="resumeId" label="简历ID" width="90" />
      <el-table-column prop="resumeTitle" label="简历标题" min-width="160" show-overflow-tooltip />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ row.exportType || '-' }}</template>
      </el-table-column>
      <el-table-column label="高清" width="80" align="center">
        <template #default="{ row }">{{ row.highDefinition ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="水印" width="80" align="center">
        <template #default="{ row }">{{ row.watermark ? '是' : '否' }}</template>
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
