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
const keyword = ref('')
const vipDialogVisible = ref(false)
const currentUser = ref(null)
const vipForm = reactive({ vipName: null, validDays: 30, aiQuota: 5, exportQuota: 3 })
const packages = ref([])

const levelOptions = computed(() => [
  { value: null, label: '免费版' },
  ...packages.value.map(p => ({ value: p.name, label: p.name }))
])

const filteredUsers = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return users.value
  return users.value.filter((item) =>
    item.username?.toLowerCase().includes(kw) || item.nickname?.toLowerCase().includes(kw)
  )
})

const refresh = async () => {
  users.value = await listAdminUsers()
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
</script>

<template>
  <section class="admin-panel-card card">
    <div class="admin-section-toolbar">
      <div>
        <h3>用户管理</h3>
        <p>管理用户会员等级、额度和基础账号状态。</p>
      </div>
      <el-input v-model="keyword" clearable placeholder="搜索用户名 / 昵称" style="width: 260px" />
    </div>

    <el-table :data="filteredUsers" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="账号" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="140" />
      <el-table-column label="会员等级" width="130">
        <template #default="{ row }">
          <el-tag :type="levelType(row.vipLevel)" size="small">{{ row.vipLevel || '免费版' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="vipExpireTime" label="到期时间" min-width="170" show-overflow-tooltip />
      <el-table-column label="AI / 导出额度" min-width="130">
        <template #default="{ row }">
          {{ row.remainingAiQuota }} / {{ row.remainingExportQuota }}
        </template>
      </el-table-column>
      <el-table-column label="今日剩余面试次数" width="140">
        <template #default="{ row }">
          <span :class="['interview-quota-cell', { 'is-empty': (row.remainingInterviewQuota ?? 0) <= 0 }]">
            {{ row.remainingInterviewQuota ?? '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
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
</style>
