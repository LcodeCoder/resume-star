<!--
  登录 / 注册页（统一入口）
  功能：个人用户登录/注册、管理员登录三合一，通过顶部分段控件「个人用户 / 管理员」就地丝滑切换，无需跳转路由
  设计：左侧品牌渐变展示区 + 右侧表单卡片；角色切换时主题色平滑过渡
-->
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { useAdminStore } from '../store/admin'
import { getUserSystemConfig, sendRegisterCode } from '../api/user'
import LegalDialog from '../components/legal/LegalDialog.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const adminStore = useAdminStore()

/** 角色：user 个人用户 | admin 管理员（支持 /login?role=admin 直达） */
const role = ref(route.query.role === 'admin' ? 'admin' : 'user')
/** 个人用户子模式：login 登录 | register 注册 */
const mode = ref('login')
const submitting = ref(false)
const registerEnabled = ref(true)
/** 是否开启邮箱验证注册（由后台系统配置控制） */
const emailVerifyEnabled = ref(false)
/** 发送验证码状态与倒计时 */
const sendingCode = ref(false)
const codeCountdown = ref(0)
let codeTimer = null

/** 条款同意（个人用户登录/注册前置），记忆上次选择 */
const agreed = ref(localStorage.getItem('rl_legal_agreed') === '1')
const legalVisible = ref(false)
const legalDoc = ref('terms')

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', nickname: '', email: '', emailCode: '' })
const adminForm = reactive({ username: '', password: '' })

/** 左侧品牌区随角色切换的文案与亮点 */
const isAdmin = computed(() => role.value === 'admin')
const asideHighlights = computed(() => isAdmin.value
  ? ['用户与会员权益管理', '模板 / AI 接口配置', '订单营收与操作审计']
  : ['拖拽式可视化简历编辑器', '多行业模板一键套用', 'AI 润色 · 纠错 · 岗位适配'])

const switchRole = (value) => {
  role.value = value
  if (value === 'admin') mode.value = 'login'
}

const openLegal = (key) => {
  legalDoc.value = key
  legalVisible.value = true
}

const ensureAgreed = () => {
  if (!agreed.value) {
    ElMessage.warning('请先阅读并勾选同意相关协议')
    return false
  }
  localStorage.setItem('rl_legal_agreed', '1')
  return true
}

onMounted(async () => {
  try {
    const config = await getUserSystemConfig()
    registerEnabled.value = config?.registerEnabled !== false
    emailVerifyEnabled.value = config?.emailVerifyEnabled === true
  } catch (e) {
    registerEnabled.value = true
    emailVerifyEnabled.value = false
  }
  if (!registerEnabled.value && mode.value === 'register') mode.value = 'login'
})

/** 邮箱格式校验 */
const isValidEmail = (email) => /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)

/** 发送注册邮箱验证码，成功后开始 60 秒倒计时 */
const handleSendCode = async () => {
  if (!isValidEmail(registerForm.email)) {
    ElMessage.warning('请输入正确的邮箱地址')
    return
  }
  sendingCode.value = true
  try {
    await sendRegisterCode(registerForm.email)
    ElMessage.success('验证码已发送，请查收邮箱')
    codeCountdown.value = 60
    codeTimer = setInterval(() => {
      codeCountdown.value -= 1
      if (codeCountdown.value <= 0) clearInterval(codeTimer)
    }, 1000)
  } catch (e) {
    // 错误信息已由 axios 拦截器提示
  } finally {
    sendingCode.value = false
  }
}

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请填写账号和密码')
    return
  }
  if (!ensureAgreed()) return
  submitting.value = true
  try {
    await userStore.login({ username: loginForm.username, password: loginForm.password })
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
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
  if (registerForm.username.length < 8) {
    ElMessage.warning('账号长度不能低于 8 位')
    return
  }
  if (registerForm.password.length < 6) {
    ElMessage.warning('密码长度至少 6 位')
    return
  }
  if (emailVerifyEnabled.value) {
    if (!isValidEmail(registerForm.email)) {
      ElMessage.warning('请输入正确的邮箱地址')
      return
    }
    if (!registerForm.emailCode) {
      ElMessage.warning('请输入邮箱验证码')
      return
    }
  }
  if (!ensureAgreed()) return
  submitting.value = true
  try {
    await userStore.register({
      username: registerForm.username,
      password: registerForm.password,
      nickname: registerForm.nickname,
      email: registerForm.email,
      emailCode: registerForm.emailCode
    })
    ElMessage.success('注册成功，已自动登录')
    router.push('/')
  } catch (e) {
    // 错误信息已由 axios 拦截器提示
  } finally {
    submitting.value = false
  }
}

const handleAdminLogin = async () => {
  if (!adminForm.username || !adminForm.password) {
    ElMessage.warning('请填写账号和密码')
    return
  }
  submitting.value = true
  try {
    await adminStore.login({ username: adminForm.username, password: adminForm.password })
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
  <div class="auth-page" :class="{ 'auth-page-admin': isAdmin }">
    <div class="auth-shell">
      <!-- 左侧品牌展示区 -->
      <aside class="auth-aside" :class="{ admin: isAdmin }">
        <div class="auth-aside-decor" aria-hidden="true">
          <span class="auth-aside-circle c1"></span>
          <span class="auth-aside-circle c2"></span>
        </div>
        <div class="auth-aside-inner">
          <div class="auth-aside-brand">
            <img class="auth-aside-logo" src="/resume-logo.png" alt="resume-lcode" />
            <span>resume-lcode</span>
          </div>
          <h2 class="auth-aside-title">{{ isAdmin ? '平台运营管理中心' : '用 AI 打造更专业的简历' }}</h2>
          <p class="auth-aside-sub">{{ isAdmin ? '集中管理用户、会员、模板与 AI 接口。' : '编辑、模板、AI 优化与一键导出，一站完成。' }}</p>
          <ul class="auth-aside-list">
            <li v-for="item in asideHighlights" :key="item">
              <span class="auth-aside-tick">✓</span>{{ item }}
            </li>
          </ul>
        </div>
      </aside>

      <!-- 右侧表单区 -->
      <div class="auth-main">
        <div class="auth-card">
          <!-- 角色分段切换 -->
          <div class="auth-role-switch">
            <button class="auth-role" :class="{ active: !isAdmin }" @click="switchRole('user')">个人用户</button>
            <button class="auth-role" :class="{ active: isAdmin }" @click="switchRole('admin')">管理员</button>
            <span class="auth-role-thumb" :class="{ right: isAdmin }"></span>
          </div>

          <transition name="auth-fade" mode="out-in">
            <!-- 个人用户 -->
            <div v-if="!isAdmin" key="user" class="auth-panel">
              <div class="auth-heading">
                <h1>{{ mode === 'login' ? '欢迎回来' : '创建账号' }}</h1>
                <p>{{ mode === 'login' ? '登录后继续编辑你的简历' : '注册一个新账号开始制作简历' }}</p>
              </div>

              <div class="auth-tabs">
                <button class="auth-tab" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
                <button v-if="registerEnabled" class="auth-tab" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
              </div>

              <el-form v-if="mode === 'login'" label-position="top" @submit.prevent="handleLogin">
                <el-form-item label="账号">
                  <el-input v-model="loginForm.username" placeholder="请输入账号" size="large" />
                </el-form-item>
                <el-form-item label="密码">
                  <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password size="large" @keyup.enter="handleLogin" />
                </el-form-item>
                <el-button type="primary" size="large" class="full-button" :loading="submitting" @click="handleLogin">登录</el-button>
              </el-form>

              <el-form v-else label-position="top" @submit.prevent="handleRegister">
                <el-form-item label="账号">
                  <el-input v-model="registerForm.username" placeholder="8-20 字符" size="large" />
                </el-form-item>
                <el-form-item label="密码">
                  <el-input v-model="registerForm.password" type="password" placeholder="至少 6 位" show-password size="large" />
                </el-form-item>
                <el-form-item v-if="emailVerifyEnabled" label="邮箱">
                  <el-input v-model="registerForm.email" placeholder="用于接收验证码" size="large" />
                </el-form-item>
                <el-form-item v-if="emailVerifyEnabled" label="邮箱验证码">
                  <div class="auth-code-row">
                    <el-input v-model="registerForm.emailCode" placeholder="6 位验证码" size="large" />
                    <el-button
                      class="auth-code-button"
                      size="large"
                      :loading="sendingCode"
                      :disabled="codeCountdown > 0"
                      @click="handleSendCode"
                    >{{ codeCountdown > 0 ? codeCountdown + 's 后重发' : '获取验证码' }}</el-button>
                  </div>
                </el-form-item>
                <el-form-item label="昵称（选填）">
                  <el-input v-model="registerForm.nickname" placeholder="留空默认使用账号" size="large" @keyup.enter="handleRegister" />
                </el-form-item>
                <el-button type="primary" size="large" class="full-button" :loading="submitting" @click="handleRegister">注册</el-button>
              </el-form>

              <div class="auth-agree">
                <el-checkbox v-model="agreed" size="small" />
                <span class="auth-agree-text">
                  我已阅读并同意
                  <button type="button" class="auth-legal-link" @click="openLegal('terms')">《用户服务协议》</button>
                  <button type="button" class="auth-legal-link" @click="openLegal('privacy')">《隐私政策》</button>
                  <button type="button" class="auth-legal-link" @click="openLegal('membership')">《会员服务协议》</button>
                </span>
              </div>
            </div>

            <!-- 管理员 -->
            <div v-else key="admin" class="auth-panel">
              <div class="auth-heading">
                <h1>管理员登录</h1>
                <p>请使用后台管理账号登录</p>
              </div>
              <el-form label-position="top" @submit.prevent="handleAdminLogin">
                <el-form-item label="管理员账号">
                  <el-input v-model="adminForm.username" placeholder="请输入管理员账号" size="large" />
                </el-form-item>
                <el-form-item label="密码">
                  <el-input v-model="adminForm.password" type="password" placeholder="请输入密码" show-password size="large" @keyup.enter="handleAdminLogin" />
                </el-form-item>
                <el-button type="primary" size="large" class="full-button" :loading="submitting" @click="handleAdminLogin">登录后台</el-button>
              </el-form>
            </div>
          </transition>

          <div class="auth-footer">
            <router-link to="/" class="auth-link">← 返回首页</router-link>
            <router-link to="/templates" class="auth-link">先逛逛模板库</router-link>
          </div>
        </div>
      </div>
    </div>

    <LegalDialog v-model:visible="legalVisible" :doc="legalDoc" />
  </div>
</template>

<style scoped>
/* 邮箱验证码输入行：输入框自适应 + 获取验证码按钮 */
.auth-code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}
.auth-code-row .el-input {
  flex: 1;
}
.auth-code-button {
  flex: 0 0 auto;
  white-space: nowrap;
}
</style>
