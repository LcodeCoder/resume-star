<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { useAdminStore } from '../store/admin'
import { getUserSystemConfig, sendRegisterCode } from '../api/user'
import ThemeSwitcher from '../components/common/ThemeSwitcher.vue'
import LegalDialog from '../components/legal/LegalDialog.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const adminStore = useAdminStore()
const role = ref(route.query.role === 'admin' ? 'admin' : 'user')
const mode = ref('login')
const submitting = ref(false)
const registerEnabled = ref(true)
const emailVerifyEnabled = ref(false)
const sendingCode = ref(false)
const codeCountdown = ref(0)
const agreed = ref(localStorage.getItem('rl_legal_agreed') === '1')
const legalVisible = ref(false)
const legalDoc = ref('terms')
let codeTimer = null

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', nickname: '', email: '', emailCode: '' })
const adminForm = reactive({ username: '', password: '' })
const isAdmin = computed(() => role.value === 'admin')

const switchRole = (next) => { role.value = next; if (next === 'admin') mode.value = 'login' }
const openLegal = (doc) => { legalDoc.value = doc; legalVisible.value = true }
const isValidEmail = (email) => /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)
const ensureAgreed = () => {
  if (!agreed.value) { ElMessage.warning('请先阅读并同意服务协议与隐私政策'); return false }
  localStorage.setItem('rl_legal_agreed', '1')
  return true
}

onMounted(async () => {
  try {
    const config = await getUserSystemConfig()
    registerEnabled.value = config?.registerEnabled !== false
    emailVerifyEnabled.value = config?.emailVerifyEnabled === true
  } catch (_) { registerEnabled.value = true }
})
onBeforeUnmount(() => { if (codeTimer) clearInterval(codeTimer) })

const handleSendCode = async () => {
  if (!isValidEmail(registerForm.email)) return ElMessage.warning('请输入有效的邮箱地址')
  sendingCode.value = true
  try {
    await sendRegisterCode(registerForm.email)
    ElMessage.success('验证码已发送，请检查邮箱')
    codeCountdown.value = 60
    codeTimer = setInterval(() => { codeCountdown.value -= 1; if (codeCountdown.value <= 0) clearInterval(codeTimer) }, 1000)
  } finally { sendingCode.value = false }
}

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) return ElMessage.warning('请填写账号和密码')
  if (!ensureAgreed()) return
  submitting.value = true
  try {
    await userStore.login(loginForm)
    ElMessage.success('登录成功，进度已同步')
    router.push(route.query.redirect || '/')
  } finally { submitting.value = false }
}

const handleRegister = async () => {
  if (registerForm.username.length < 8) return ElMessage.warning('账号至少需要 8 个字符')
  if (registerForm.password.length < 6) return ElMessage.warning('密码至少需要 6 个字符')
  if (emailVerifyEnabled.value && (!isValidEmail(registerForm.email) || !registerForm.emailCode)) return ElMessage.warning('请填写有效邮箱与验证码')
  if (!ensureAgreed()) return
  submitting.value = true
  try {
    await userStore.register(registerForm)
    ElMessage.success('账号已建立，欢迎加入')
    router.push('/')
  } finally { submitting.value = false }
}

const handleAdminLogin = async () => {
  if (!adminForm.username || !adminForm.password) return ElMessage.warning('请填写管理员账号和密码')
  submitting.value = true
  try {
    await adminStore.login(adminForm)
    ElMessage.success('控制中心已解锁')
    router.push('/admin')
  } finally { submitting.value = false }
}
</script>

<template>
  <main class="gateway">
    <header class="gateway-top">
      <button class="gateway-brand" type="button" @click="router.push('/')"><span class="gateway-star"></span><b>履历星图</b><small>RESUME ORBIT</small></button>
      <ThemeSwitcher compact />
    </header>

    <section class="gateway-scene">
      <div class="gateway-story">
        <span class="coordinate">E 31.23° · N 121.47° · 求职航线</span>
        <transition name="story-swap" mode="out-in">
          <div :key="isAdmin ? 'admin' : 'user'" class="story-text">
            <h1>{{ isAdmin ? '维护每一条可靠航线。' : '从你的经历，抵达下一站。' }}</h1>
            <p>{{ isAdmin ? '集中检查用户、模板、订单与 AI 服务，让平台持续稳定运行。' : '建立一份可以持续生长的主简历，再为每个目标岗位调整方向。' }}</p>
          </div>
        </transition>
        <ol>
          <li><b>整理</b><span>把分散的项目与经历收回同一坐标</span></li>
          <li><b>校准</b><span>用模板和 AI 调整内容的清晰度</span></li>
          <li><b>出发</b><span>导出、投递，并为面试准备例证</span></li>
        </ol>
        <div class="gateway-orbit" aria-hidden="true"><i></i><i></i><i></i><b></b></div>
      </div>

      <div class="gateway-console">
        <div class="role-switch" aria-label="登录身份">
          <button :class="{ active: !isAdmin }" @click="switchRole('user')">个人用户</button>
          <button :class="{ active: isAdmin }" @click="switchRole('admin')">平台管理员</button>
        </div>

        <transition name="console-swap" mode="out-in">
          <div v-if="!isAdmin" key="user" class="console-panel">
            <header><div><h2>{{ mode === 'login' ? '继续你的航程' : '建立个人坐标' }}</h2><p>{{ mode === 'login' ? '输入账号，同步已保存的简历与进度。' : '创建账号后，内容会安全同步到云端。' }}</p></div><span>{{ mode === 'login' ? 'LOGIN' : 'REGISTER' }}</span></header>
            <div class="mode-tabs">
              <button :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
              <button v-if="registerEnabled" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
            </div>

            <el-form v-if="mode === 'login'" label-position="top" @submit.prevent="handleLogin">
              <el-form-item label="账号"><el-input v-model="loginForm.username" size="large" autocomplete="username" placeholder="输入账号" /></el-form-item>
              <el-form-item label="密码"><el-input v-model="loginForm.password" size="large" type="password" autocomplete="current-password" show-password placeholder="输入密码" @keyup.enter="handleLogin" /></el-form-item>
              <el-button class="submit-button" type="primary" size="large" :loading="submitting" @click="handleLogin">同步进度并进入</el-button>
            </el-form>

            <el-form v-else label-position="top" @submit.prevent="handleRegister">
              <div class="form-pair"><el-form-item label="账号"><el-input v-model="registerForm.username" size="large" placeholder="至少 8 个字符" /></el-form-item><el-form-item label="称呼（选填）"><el-input v-model="registerForm.nickname" size="large" placeholder="如何称呼你" /></el-form-item></div>
              <el-form-item label="密码"><el-input v-model="registerForm.password" size="large" type="password" show-password placeholder="至少 6 个字符" /></el-form-item>
              <el-form-item v-if="emailVerifyEnabled" label="邮箱"><el-input v-model="registerForm.email" size="large" placeholder="接收验证码的邮箱" /></el-form-item>
              <el-form-item v-if="emailVerifyEnabled" label="邮箱验证码"><div class="code-row"><el-input v-model="registerForm.emailCode" size="large" placeholder="6 位验证码" /><el-button :loading="sendingCode" :disabled="codeCountdown > 0" @click="handleSendCode">{{ codeCountdown > 0 ? `${codeCountdown}s` : '发送验证码' }}</el-button></div></el-form-item>
              <el-button class="submit-button" type="primary" size="large" :loading="submitting" @click="handleRegister">创建账号并进入</el-button>
            </el-form>
          </div>

          <div v-else key="admin" class="console-panel">
            <header><div><h2>进入控制中心</h2><p>此入口仅供平台运营与维护人员使用。</p></div><span>ADMIN</span></header>
            <el-form label-position="top" @submit.prevent="handleAdminLogin">
              <el-form-item label="管理员账号"><el-input v-model="adminForm.username" size="large" autocomplete="username" placeholder="输入管理员账号" /></el-form-item>
              <el-form-item label="密码"><el-input v-model="adminForm.password" size="large" type="password" autocomplete="current-password" show-password placeholder="输入密码" @keyup.enter="handleAdminLogin" /></el-form-item>
              <el-button class="submit-button" type="primary" size="large" :loading="submitting" @click="handleAdminLogin">验证身份</el-button>
            </el-form>
          </div>
        </transition>

        <label v-if="!isAdmin" class="legal-check"><input v-model="agreed" type="checkbox"><span>我已阅读并同意 <button type="button" @click="openLegal('terms')">服务协议</button> 与 <button type="button" @click="openLegal('privacy')">隐私政策</button></span></label>
        <button class="back-home" type="button" @click="router.push('/')">暂不登录，返回探索模式</button>
      </div>
    </section>
    <LegalDialog v-model:visible="legalVisible" :doc="legalDoc" />
  </main>
</template>

<style scoped>
.gateway { min-height: 100vh; display: flex; flex-direction: column; padding: 22px clamp(18px,4vw,64px); background: var(--space); overflow: hidden; }.gateway-top { z-index: 2; display: flex; align-items: center; justify-content: space-between; }.gateway-brand { display: flex; align-items: center; gap: 9px; border: 0; background: transparent; cursor: pointer; }.gateway-brand small { color: var(--muted); font-size: 9px; letter-spacing: .14em; }.gateway-star { width: 24px; height: 24px; border: 6px solid var(--accent-soft); border-radius: 50%; background: var(--accent); }.gateway-top :deep(.theme-trigger) { width: auto; }
.gateway-scene { width: min(1180px,100%); flex: 1; display: grid; grid-template-columns: 1fr minmax(420px, .8fr); align-items: center; gap: clamp(50px,8vw,130px); margin: 30px auto; }.gateway-story { position: relative; max-width: 600px; }.coordinate { color: var(--muted); font: 700 10px/1.4 ui-monospace,monospace; letter-spacing: .12em; }.gateway-story h1 { max-width: 9ch; margin: 24px 0; font-size: clamp(44px,6vw,76px); line-height: 1.04; letter-spacing: -.04em; }.gateway-story > p, .story-text > p { max-width: 46ch; color: var(--ink-2); font-size: 16px; line-height: 1.8; }.story-text { display: block; }.gateway-story ol { display: grid; gap: 12px; margin: 36px 0 0; padding: 0; list-style: none; }.gateway-story li { display: grid; grid-template-columns: 48px 1fr; color: var(--ink-2); }.gateway-story li b { color: var(--ink); }.gateway-orbit { position: absolute; z-index: -1; width: 520px; height: 520px; left: -240px; top: 50%; border: 1px solid var(--line); border-radius: 50%; transform: translateY(-50%); }.gateway-orbit i { position: absolute; width: 7px; height: 7px; border-radius: 50%; background: var(--accent); }.gateway-orbit i:nth-child(1) { right: 61px; top: 80px; }.gateway-orbit i:nth-child(2) { right: -3px; top: 250px; background: var(--star); }.gateway-orbit i:nth-child(3) { right: 70px; bottom: 65px; background: var(--success); }
.gateway-console { padding: clamp(24px,4vw,46px); border: 1px solid var(--line); border-radius: var(--radius-lg); background: var(--surface); box-shadow: var(--shadow); }.role-switch, .mode-tabs { display: grid; grid-template-columns: 1fr 1fr; gap: 3px; padding: 3px; background: var(--surface-2); border-radius: var(--radius-sm); }.role-switch button, .mode-tabs button { min-height: 35px; border: 0; border-radius: 4px; background: transparent; color: var(--muted); cursor: pointer; }.role-switch button.active, .mode-tabs button.active { background: var(--surface); color: var(--ink); font-weight: 750; }.console-panel header { display: flex; justify-content: space-between; gap: 20px; margin: 30px 0 22px; }.console-panel h2 { margin: 0; font-size: 24px; }.console-panel header p { margin: 5px 0 0; color: var(--muted); }.console-panel header > span { color: var(--accent); font: 750 9px/1.4 ui-monospace,monospace; letter-spacing: .15em; }.mode-tabs { display: flex; gap: 20px; padding: 0; margin-bottom: 22px; border-bottom: 1px solid var(--line); background: none; border-radius: 0; }.mode-tabs button { padding: 0 0 8px; border-radius: 0; }.mode-tabs button.active { margin-bottom: -1px; border-bottom: 2px solid var(--accent); background: none; }.submit-button { width: 100%; margin-top: 4px; }.form-pair { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }.code-row { width: 100%; display: grid; grid-template-columns: 1fr auto; gap: 8px; }.legal-check { display: flex; align-items: flex-start; gap: 8px; margin-top: 16px; color: var(--muted); font-size: 11px; }.legal-check input { accent-color: var(--accent); }.legal-check button { padding: 0; border: 0; background: transparent; color: var(--accent); cursor: pointer; }.back-home { display: block; margin: 20px auto 0; border: 0; background: transparent; color: var(--muted); font-size: 12px; cursor: pointer; }.back-home:hover { color: var(--ink); }
@media (max-width: 900px) { .gateway-scene { grid-template-columns: 1fr; gap: 36px; }.gateway-story { padding-top: 30px; }.gateway-story h1 { max-width: 12ch; font-size: 48px; }.gateway-story ol { display: none; }.gateway-console { width: min(560px,100%); }.gateway-orbit { opacity: .55; } }
@media (max-width: 520px) { .gateway { padding: 14px 12px; }.gateway-brand small { display: none; }.gateway-story h1 { font-size: 38px; }.gateway-story > p, .story-text > p { font-size: 14px; }.gateway-console { padding: 22px 16px; }.form-pair { grid-template-columns: 1fr; gap: 0; } }

/* 身份切换过渡：左右文案与表单面板用 out-in 错峰淡入，避免硬切闪烁。
   左侧文案随身份横向轻滑，右侧表单整块淡入淡出；统一项目缓动。
   使用 out-in 模式，旧元素先退场再进新元素，无需绝对定位、不依赖位置猜测。 */
.story-swap-enter-active, .story-swap-leave-active,
.console-swap-enter-active, .console-swap-leave-active { transition: opacity 220ms cubic-bezier(.16,1,.3,1), transform 220ms cubic-bezier(.16,1,.3,1); }
.story-swap-leave-to { opacity: 0; transform: translateX(-12px); }
.story-swap-enter-from, .console-swap-enter-from { opacity: 0; transform: translateX(12px); }
.console-swap-leave-to { opacity: 0; transform: translateX(-12px); }
@media (prefers-reduced-motion: reduce) { .story-swap-enter-active, .story-swap-leave-active, .console-swap-enter-active, .console-swap-leave-active { transition: none; } }
</style>
