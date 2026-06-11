<!--
  后台用户管理 Tab
  功能：查询用户、调整会员等级、重置密码、删除非演示用户
-->
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAdminUser, listAdminUsers, resetUserPassword, updateUserVip } from '../../api/admin'

const users = ref([])
const keyword = ref('')
const vipDialogVisible = ref(false)
const currentUser = ref(null)
const vipForm = reactive({ levelCode: 'FREE', validDays: 30 })

const levelLabels = {
  FREE: '免费版',
  BASIC: '基础会员',
  PRO: '专业会员',
  ENTERPRISE: '企业会员'
}

const levelOptions = [
  { value: 'FREE', label: '免费版' },
  { value: 'BASIC', label: '基础会员' },
  { value: 'PRO', label: '专业会员' },
  { value: 'ENTERPRISE', label: '企业会员' }
]

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

onMounted(refresh)

const openVipDialog = (user) => {
  currentUser.value = user
  vipForm.levelCode = user.vipLevel || 'FREE'
  vipForm.validDays = 30
  vipDialogVisible.value = true
}

const saveVip = async () => {
  if (!currentUser.value) return
  await updateUserVip(currentUser.value.id, { ...vipForm })
  ElMessage.success('会员信息已更新')
  vipDialogVisible.value = false
  await refresh()
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
  if (level === 'ENTERPRISE') return 'warning'
  if (level === 'PRO') return 'success'
  if (level === 'BASIC') return 'primary'
  return 'info'
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

    <el-table :data="filteredUsers" stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="账号" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="140" />
      <el-table-column label="会员等级" width="130">
        <template #default="{ row }">
          <el-tag :type="levelType(row.vipLevel)" size="small">{{ levelLabels[row.vipLevel] || row.vipLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="vipExpireTime" label="到期时间" min-width="170" show-overflow-tooltip />
      <el-table-column label="AI / 导出额度" min-width="150">
        <template #default="{ row }">
          {{ row.remainingAiQuota }} / {{ row.remainingExportQuota }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openVipDialog(row)">会员</el-button>
          <el-button size="small" @click="handleResetPassword(row)">重置密码</el-button>
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
      <el-form-item label="会员等级">
        <el-select v-model="vipForm.levelCode" style="width: 100%">
          <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="有效天数">
        <el-input-number v-model="vipForm.validDays" :min="1" :max="3650" :disabled="vipForm.levelCode === 'FREE'" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="vipDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveVip">保存</el-button>
    </template>
  </el-dialog>
</template>
