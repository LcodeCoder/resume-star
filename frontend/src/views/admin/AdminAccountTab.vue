<!--
  后台「账号设置」Tab
  功能：当前登录管理员自助修改账号 / 昵称 / 密码
  说明：修改需校验当前密码；账号或密码任一可单独修改，留空则不变更。
-->
<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAdminStore } from '../../store/admin'

const adminStore = useAdminStore()
const submitting = ref(false)

/** 表单：当前密码必填用于身份校验，其余留空则不改 */
const form = reactive({
  newUsername: adminStore.profile?.username || '',
  newNickname: adminStore.profile?.nickname || '',
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const handleSubmit = async () => {
  if (!form.currentPassword) {
    ElMessage.warning('请填写当前密码以验证身份')
    return
  }
  if (form.newPassword && form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (form.newPassword && form.newPassword.length < 6) {
    ElMessage.warning('新密码长度至少 6 位')
    return
  }
  if (form.newUsername && form.newUsername.length < 4) {
    ElMessage.warning('账号长度至少 4 位')
    return
  }
  submitting.value = true
  try {
    await adminStore.updateProfile({
      currentPassword: form.currentPassword,
      newUsername: form.newUsername,
      newNickname: form.newNickname,
      newPassword: form.newPassword
    })
    ElMessage.success('账号信息已更新')
    // 清空密码相关字段，账号/昵称回填为最新值
    form.currentPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    form.newUsername = adminStore.profile?.username || ''
    form.newNickname = adminStore.profile?.nickname || ''
  } catch (e) {
    // 错误信息已由 axios 拦截器提示
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="admin-account-layout">
    <div class="admin-panel-card card">
      <div class="admin-card-title">
        <h3>账号设置</h3>
        <span>修改当前管理员的登录账号、昵称与密码，需验证当前密码。</span>
      </div>
      <el-form label-position="top" class="admin-account-form" @submit.prevent="handleSubmit">
        <el-form-item label="登录账号">
          <el-input v-model="form.newUsername" placeholder="至少 4 位" size="large" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.newNickname" placeholder="显示用昵称" size="large" />
        </el-form-item>
        <el-divider />
        <el-form-item label="当前密码（必填）">
          <el-input v-model="form.currentPassword" type="password" placeholder="验证身份" show-password size="large" />
        </el-form-item>
        <el-form-item label="新密码（留空则不修改）">
          <el-input v-model="form.newPassword" type="password" placeholder="至少 6 位" show-password size="large" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入新密码" show-password size="large" />
        </el-form-item>
        <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">保存修改</el-button>
      </el-form>
    </div>
  </section>
</template>

<style scoped>
.admin-account-form {
  max-width: 460px;
}
</style>
