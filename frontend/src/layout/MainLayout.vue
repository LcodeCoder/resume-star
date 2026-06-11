<!--
  主布局组件
  功能：提供顶部导航、全局品牌区、会员信息快捷入口和页面内容容器
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { useAdminStore } from '../store/admin'
import LegalDialog from '../components/legal/LegalDialog.vue'
import AnnouncementDialog from '../components/announcement/AnnouncementDialog.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const adminStore = useAdminStore()

const isAdmin = computed(() => adminStore.profile?.role === 'ADMIN')
const activePath = computed(() => route.path)

/** 页脚法律条款弹窗 */
const legalVisible = ref(false)
const legalDoc = ref('terms')
const openLegal = (key) => {
  legalDoc.value = key
  legalVisible.value = true
}
/** 当前年份（版权显示） */
const year = new Date().getFullYear()

onMounted(async () => {
  // 先恢复管理员登录态；管理员已登录时不再主动请求用户资料，避免被用户态 401 误跳转
  if (!adminStore.profile) await adminStore.loadProfile()
  if (!adminStore.isLoggedIn && !userStore.authReady) {
    await userStore.loadProfile()
  }
})

const go = (path) => router.push(path)

const handleUserClick = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    await ElMessageBox.confirm('确定要登出吗？', '登出确认', { type: 'warning' })
    await userStore.logout()
    ElMessage.success('已登出')
    router.push('/login')
  } catch (e) {
    // 取消
  }
}

const handleAdminClick = async () => {
  try {
    await ElMessageBox.confirm('确定要退出管理员登录吗？', '退出确认', { type: 'warning' })
    await adminStore.logout()
    ElMessage.success('管理员已退出')
    router.push('/admin/login')
  } catch (e) {
    // 取消
  }
}
</script>

<template>
  <div class="app-shell">
    <header class="topbar card">
      <div class="brand" @click="go('/')">
        <img class="brand-logo" src="/resume-logo.png" alt="resume-lcode" />
        <div>
          <div class="brand-title">resume-lcode</div>
          <div class="brand-subtitle">智能简历制作与 AI 优化平台</div>
        </div>
      </div>
      <nav class="nav-list">
        <button
          class="nav-button"
          :class="{ active: activePath === '/' }"
          @click="go('/')"
        >
          首页
        </button>
        <button
          v-if="!isAdmin"
          class="nav-button"
          :class="{ active: activePath === '/editor' }"
          @click="go('/editor')"
        >
          简历编辑器
        </button>
        <button
          class="nav-button"
          :class="{ active: activePath === '/templates' }"
          @click="go('/templates')"
        >
          模板库
        </button>
        <button
          v-if="!isAdmin"
          class="nav-button"
          :class="{ active: activePath === '/profile' }"
          @click="go('/profile')"
        >
          个人中心
        </button>
        <button
          v-if="isAdmin"
          class="nav-button"
          :class="{ active: activePath === '/admin' }"
          @click="go('/admin')"
        >
          后台管理
        </button>
      </nav>
      <div v-if="isAdmin" class="topbar-user" @click="handleAdminClick">
        <div class="quota-text">管理员：{{ adminStore.profile?.nickname || adminStore.profile?.username }}</div>
        <div class="quota-subtext">当前为后台管理模式｜点击退出管理员登录</div>
      </div>
      <div v-else-if="userStore.isLoggedIn" class="topbar-user" @click="handleUserClick">
        <div class="quota-text">{{ userStore.profile?.nickname }}｜{{ userStore.vipLevelLabel }}</div>
        <div class="quota-subtext">AI 剩余 {{ userStore.remainingAiQuota }} 次｜导出剩余 {{ userStore.remainingExportQuota }} 次｜点击登出</div>
      </div>
      <div v-else class="topbar-user" @click="router.push('/login')">
        <div class="quota-text">未登录</div>
        <div class="quota-subtext">点击登录 / 注册</div>
      </div>
    </header>
    <main class="page-container">
      <router-view v-slot="{ Component, route }">
        <transition name="page-route" mode="out-in">
          <div :key="route.name || route.path" class="page-route-frame">
            <component :is="Component" />
          </div>
        </transition>
      </router-view>
    </main>

    <!-- 全局页脚：版权 + 法律条款 + 备案号占位 -->
    <footer class="app-footer">
      <div class="app-footer-links">
        <button type="button" class="app-footer-link" @click="openLegal('terms')">用户服务协议</button>
        <span class="app-footer-sep">·</span>
        <button type="button" class="app-footer-link" @click="openLegal('privacy')">隐私政策</button>
        <span class="app-footer-sep">·</span>
        <button type="button" class="app-footer-link" @click="openLegal('membership')">会员服务协议</button>
      </div>
      <div class="app-footer-copy">
        © {{ year }} resume-lcode · 【请填写：运营方名称】
        <span class="app-footer-icp">【ICP 备案号：请上线后填写】</span>
      </div>
    </footer>

    <LegalDialog v-model:visible="legalVisible" :doc="legalDoc" />

    <!-- 站内公告：进站自动拉取启用中的公告并弹出，已读版本不再弹 -->
    <AnnouncementDialog />
  </div>
</template>

