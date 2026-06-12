<!--
  后台操作审计日志 Tab
  功能：展示管理员的关键操作记录（改会员、删用户、重置密码、模板与权限变更等）
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAuditLogs, clearAuditLogs } from '../../api/admin'

const logs = ref([])
const keyword = ref('')

const refresh = async () => {
  logs.value = await listAuditLogs() || []
}

onMounted(refresh)

const filteredLogs = computed(() => {
  const kw = keyword.value.trim()
  if (!kw) return logs.value
  return logs.value.filter((item) =>
    (item.action || '').includes(kw) || (item.target || '').includes(kw) || (item.operator || '').includes(kw)
  )
})

const handleClear = async () => {
  await ElMessageBox.confirm('确定清空全部审计日志吗？', '清空确认', { type: 'warning' })
  await clearAuditLogs()
  ElMessage.success('已清空')
  await refresh()
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '—')
</script>

<template>
  <section class="admin-panel-card card">
    <div class="admin-section-toolbar">
      <div>
        <h3>操作审计日志</h3>
        <p>记录后台关键操作，便于追溯。最多保留最近 500 条。</p>
      </div>
      <div style="display: flex; gap: 12px;">
        <el-input v-model="keyword" clearable placeholder="搜索操作 / 对象 / 操作人" style="width: 260px" />
        <el-button type="danger" plain @click="handleClear">清空日志</el-button>
      </div>
    </div>

    <el-table :data="filteredLogs" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="operator" label="操作人" width="130" />
      <el-table-column prop="action" label="操作" width="160" />
      <el-table-column prop="target" label="对象" width="150" />
      <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
      <el-table-column label="时间" min-width="180">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!filteredLogs.length" description="暂无操作记录" />
  </section>
</template>
