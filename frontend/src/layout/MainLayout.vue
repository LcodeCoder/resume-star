<!--
  主布局组件
  功能：提供顶部导航、全局品牌区、会员信息快捷入口和页面内容容器
-->
<script setup>
import { computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { useAdminStore } from '../store/admin'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const adminStore = useAdminStore()

const isAdmin = computed(() => adminStore.profile?.role === 'ADMIN')
const activePath = computed(() => route.path)

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
      <div>
        <div class="brand-title">resume-lcode</div>
        <div class="brand-subtitle">智能简历制作与 AI 优化平台</div>
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
  </div>
</template>

