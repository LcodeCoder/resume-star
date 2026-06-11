<!--
  用户登录/注册页
  功能：账号密码登录、新用户注册（同一页面切换 Tab）
  设计：居中卡片 + 浅灰背景，与产品端整体浅色风格一致
-->
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { getUserSystemConfig } from '../api/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const mode = ref('login') // login | register
const submitting = ref(false)
const registerEnabled = ref(true)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', nickname: '' })

onMounted(async () => {
  try {
    const config = await getUserSystemConfig()
    registerEnabled.value = config?.registerEnabled !== false
  } catch (e) {
    registerEnabled.value = true
  }
  if (!registerEnabled.value) mode.value = 'login'
})

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请填写账号和密码')
    return
  }
  submitting.value = true
  try {
    await userStore.login({ username: loginForm.username, password: loginForm.password })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    // 错误信息已由 axios 拦截器提示
  } finally {
    submitting.value = false
  }
}

const handleRegister = async () => {
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning('请填写账号和密码')
    return
  }
  if (registerForm.password.length < 6) {
    ElMessage.warning('密码长度至少 6 位')
    return
  }
  submitting.value = true
  try {
    await userStore.register({
      username: registerForm.username,
      password: registerForm.password,
      nickname: registerForm.nickname
    })
    ElMessage.success('注册成功，已自动登录')
    router.push('/')
  } catch (e) {
    // 错误信息已由 axios 拦截器提示
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card card">
      <div class="auth-brand">
        <img class="auth-logo-img" src="/resume-logo.png" alt="resume-lcode" />
        <h1>resume-lcode</h1>
        <p>智能简历制作与 AI 优化平台</p>
      </div>

      <div class="auth-tabs">
        <button class="auth-tab" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
        <button v-if="registerEnabled" class="auth-tab" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
      </div>

      <el-form v-if="mode === 'login'" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="账号">
          <el-input v-model="loginForm.username" placeholder="演示账号：demo" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" placeholder="演示密码：demo123" show-password size="large" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" size="large" class="full-button" :loading="submitting" @click="handleLogin">登录</el-button>
      </el-form>

      <el-form v-else label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="账号">
          <el-input v-model="registerForm.username" placeholder="3-20 字符" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="registerForm.password" type="password" placeholder="至少 6 位" show-password size="large" />
        </el-form-item>
        <el-form-item label="昵称（选填）">
          <el-input v-model="registerForm.nickname" placeholder="留空默认使用账号" size="large" @keyup.enter="handleRegister" />
        </el-form-item>
        <el-button type="primary" size="large" class="full-button" :loading="submitting" @click="handleRegister">注册</el-button>
      </el-form>

      <div class="auth-footer">
        <router-link to="/admin/login" class="auth-link">管理员入口</router-link>
        <router-link to="/" class="auth-link">返回首页</router-link>
      </div>
    </div>
  </div>
</template>
