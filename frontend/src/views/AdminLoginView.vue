<!--
  管理员独立登录页
  功能：管理员账号密码登录，登录后跳转后台管理
  设计：深色配色与用户端区分，强调管理身份
-->
<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAdminStore } from '../store/admin'

const router = useRouter()
const adminStore = useAdminStore()

const form = reactive({ username: '', password: '' })
const submitting = ref(false)

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写账号和密码')
    return
  }
  submitting.value = true
  try {
    await adminStore.login({ username: form.username, password: form.password })
    ElMessage.success('管理员登录成功')
    router.push('/admin')
  } catch (e) {
    // 错误信息已由 axios 拦截器提示
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page auth-page-admin">
    <div class="auth-card auth-card-dark card">
      <div class="auth-brand">
        <div class="auth-logo auth-logo-dark">A</div>
        <h1>管理后台</h1>
        <p>resume-lcode 平台运营管理入口</p>
      </div>

      <el-form label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="管理员账号">
          <el-input v-model="form.username" placeholder="默认：admin" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="默认：admin123" show-password size="large" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" size="large" class="full-button" :loading="submitting" @click="handleLogin">登录后台</el-button>
      </el-form>

      <div class="auth-footer">
        <router-link to="/login" class="auth-link">用户登录</router-link>
        <router-link to="/" class="auth-link">返回首页</router-link>
      </div>
    </div>
  </div>
</template>
