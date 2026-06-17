<!--
  后台用户管理 Tab
  功能：查询用户、调整会员等级、重置密码、删除非演示用户
-->
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAdminUser, listAdminUsers, resetUserPassword, updateUserVip, setUserBanned } from '../../api/admin'
import { listMemberPackages } from '../../api/member'

const users = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(false)
const vipDialogVisible = ref(false)
const currentUser = ref(null)
const vipForm = reactive({ vipName: null, validDays: 30, aiQuota: 5, exportQuota: 3 })
const packages = ref([])

const levelOptions = computed(() => [
  { value: null, label: '免费版' },
  ...packages.value.map(p => ({ value: p.name, label: p.name }))
])

const refresh = async () => {
  loading.value = true
  try {
    const res = await listAdminUsers({ page: page.value, size: pageSize.value, keyword: keyword.value.trim() })
    users.value = res?.records ?? []
    total.value = res?.total ?? 0
  } finally {
    loading.value = false
  }
}

/** 关键字搜索：回到第 1 页后按后端分页查询 */
const handleSearch = () => {
  page.value = 1
  refresh()
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

onMounted(async () => {
  packages.value = await listMemberPackages()
  await refresh()
})

const openVipDialog = (user) => {
  currentUser.value = user
  vipForm.vipName = user.vipLevel || null
  vipForm.validDays = 30
  vipForm.aiQuota = user.remainingAiQuota ?? 5
  vipForm.exportQuota = user.remainingExportQuota ?? 3
  vipDialogVisible.value = true
}

const saveVip = async () => {
  if (!currentUser.value) return
  await updateUserVip(currentUser.value.id, { ...vipForm })
  ElMessage.success('会员信息已更新')
  vipDialogVisible.value = false
  await refresh()
}

/** 封禁 / 解封用户 */
const handleToggleBan = async (user) => {
  const banned = !user.banned
  await ElMessageBox.confirm(
    `确定${banned ? '封禁' : '解封'}用户「${user.username}」吗？${banned ? '封禁后该用户将无法登录。' : ''}`,
    banned ? '封禁确认' : '解封确认',
    { type: 'warning' }
  )
  const res = await setUserBanned(user.id, { banned })
  if (res === banned) {
    ElMessage.success(banned ? '用户已封禁' : '用户已解封')
    await refresh()
  } else {
    ElMessage.warning('操作失败：演示账号不可封禁或用户不存在')
  }
}

const handleResetPassword = async (user) => {
  const { value } = await ElMessageBox.prompt(`为用户「${user.username}」设置新密码`, '重置密码', {
    confirmButtonText: '确认重置',
    cancelButtonText: '取消',
    inputPattern: /^.{6,}$/,
    inputErrorMessage: '密码至少 6 位'
  })
  const ok = await resetUserPassword(user.id, { newPassword: value })
  if (ok) ElMessage.success('密码已重置')
  else ElMessage.warning('重置失败，请检查用户是否存在')
}

const handleDelete = async (user) => {
  await ElMessageBox.confirm(`确定删除用户「${user.username}」吗？演示账号不可删除。`, '删除确认', { type: 'warning' })
  const ok = await deleteAdminUser(user.id)
  if (ok) {
    ElMessage.success('用户已删除')
    // 删除后当前页可能为空，回退一页
    if (users.value.length === 1 && page.value > 1) page.value -= 1
    await refresh()
  } else {
    ElMessage.warning('删除失败：演示账号或用户不存在')
  }
}

const levelType = (level) => {
  if (!level) return 'info'
  if (level.includes('企业')) return 'warning'
  if (level.includes('专业')) return 'success'
  return 'primary'
}

/** 额度展示：今日已用 / 每日总额（不限额显示 ∞） */
const quotaText = (used, total, unlimited) => {
  const u = used ?? 0
  if (unlimited) return `${u} / ∞`
  return `${u} / ${total ?? 0}`
}
</script>

<template>
  <section class="admin-panel-card card">
    <div class="admin-section-toolbar">
      <div>
        <h3>用户管理</h3>
        <p>管理用户会员等级、额度和基础账号状态。</p>
      </div>
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索用户名 / 昵称 / 邮箱"
        style="width: 260px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #append>
          <el-button @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <el-table :data="users" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="账号" min-width="110" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column label="邮箱" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <span :class="{ 'empty-cell': !row.email }">{{ row.email || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="会员等级" width="120">
        <template #default="{ row }">
          <el-tag :type="levelType(row.vipLevel)" size="small">{{ row.vipLevel || '免费版' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="vipExpireTime" label="到期时间" min-width="160" show-overflow-tooltip />
      <el-table-column label="AI额度(今日已用/每日总额)" width="180" align="center">
        <template #default="{ row }">
          <span class="quota-cell">{{ quotaText(row.aiUsedToday, row.aiDailyTotal, row.aiUnlimited) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="导出额度(今日已用/每日总额)" width="190" align="center">
        <template #default="{ row }">
          <span class="quota-cell">{{ quotaText(row.exportUsedToday, row.exportDailyTotal, row.exportUnlimited) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="今日剩余面试次数" width="130" align="center">
        <template #default="{ row }">
          <span :class="['interview-quota-cell', { 'is-empty': (row.remainingInterviewQuota ?? 0) <= 0 }]">
            {{ row.remainingInterviewQuota ?? '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="84">
        <template #default="{ row }">
          <el-tag :type="row.banned ? 'danger' : 'success'" size="small">{{ row.banned ? '已封禁' : '正常' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openVipDialog(row)">会员</el-button>
          <el-button size="small" @click="handleResetPassword(row)">重置密码</el-button>
          <el-button size="small" :type="row.banned ? 'success' : 'warning'" plain @click="handleToggleBan(row)">
            {{ row.banned ? '解封' : '封禁' }}
          </el-button>
          <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
        </template>
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

  <el-dialog v-model="vipDialogVisible" title="调整会员权益" width="420px">
    <el-form label-position="top">
      <el-form-item label="用户">
        <el-input :model-value="currentUser ? `${currentUser.nickname || currentUser.username}（${currentUser.username}）` : ''" disabled />
      </el-form-item>
      <el-form-item label="会员套餐">
        <el-select v-model="vipForm.vipName" style="width: 100%">
          <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="有效天数（从今天起算到期时间）">
        <el-input-number v-model="vipForm.validDays" :min="1" :max="3650" :disabled="!vipForm.vipName" />
      </el-form-item>
      <div class="vip-quota-row">
        <el-form-item label="每日 AI 次数">
          <el-input-number v-model="vipForm.aiQuota" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="每日导出次数">
          <el-input-number v-model="vipForm.exportQuota" :min="0" :max="9999" />
        </el-form-item>
      </div>
      <p class="vip-quota-hint">留空使用等级默认额度；手动填写可为该用户单独定制权益。</p>
    </el-form>
    <template #footer>
      <el-button @click="vipDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveVip">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.interview-quota-cell {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
  color: #16a34a;
}
.interview-quota-cell.is-empty {
  color: #94a3b8;
}
.quota-cell {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}
.empty-cell {
  color: #c0c4cc;
}
.admin-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
