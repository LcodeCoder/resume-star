<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { useAdminStore } from '../store/admin'
import ThemeSwitcher from '../components/common/ThemeSwitcher.vue'
import LegalDialog from '../components/legal/LegalDialog.vue'
import AnnouncementDialog from '../components/announcement/AnnouncementDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const adminStore = useAdminStore()
const mobileOpen = ref(false)
const legalVisible = ref(false)
const legalDoc = ref('terms')
const year = new Date().getFullYear()

const isAdmin = computed(() => adminStore.profile?.role === 'ADMIN')
const identity = computed(() => isAdmin.value
  ? (adminStore.profile?.nickname || adminStore.profile?.username || '管理员')
  : (userStore.profile?.nickname || '访客'))

const navItems = computed(() => isAdmin.value ? [
  { path: '/', label: '总览', glyph: '◉', hue: '255' },
  { path: '/templates', label: '模板星库', glyph: '◇', hue: '155' },
  { path: '/community', label: '社区信号', glyph: '⌁', hue: '300' },
  { path: '/admin', label: '控制中心', glyph: '⊹', hue: '27' }
] : [
  { path: '/', label: '任务总览', glyph: '◉', hue: '255' },
  { path: '/editor', label: '简历工坊', glyph: '✦', hue: '75' },
  { path: '/templates', label: '模板星库', glyph: '◇', hue: '155' },
  { path: '/interview', label: '面试舱', glyph: '◎', hue: '200' },
  { path: '/community', label: '社区信号', glyph: '⌁', hue: '300' },
  { path: '/profile', label: '个人档案', glyph: '○', hue: '340' },
  { path: '/settings', label: '系统设置', glyph: '⌘', hue: '27' }
])

const active = (path) => path === '/' ? route.path === '/' : route.path.startsWith(path)
const go = (path) => { router.push(path); mobileOpen.value = false }
const openLegal = (doc) => { legalDoc.value = doc; legalVisible.value = true }

onMounted(async () => {
  if (route.path.startsWith('/admin') && !adminStore.profile) await adminStore.loadProfile()
})

const handleIdentity = async () => {
  if (!userStore.isLoggedIn && !adminStore.isLoggedIn) return router.push('/login')
  try {
    await ElMessageBox.confirm('结束当前会话并返回登录页？', '退出登录', { type: 'warning', confirmButtonText: '退出', cancelButtonText: '留在这里' })
    if (isAdmin.value) await adminStore.logout()
    else await userStore.logout()
    ElMessage.success('已安全退出')
    router.push('/login')
  } catch (_) { /* cancelled */ }
}
</script>

<template>
  <div class="orbit-app">
    <button class="mobile-orbit-toggle" type="button" :aria-expanded="mobileOpen" @click="mobileOpen = !mobileOpen">
      <span></span><span></span><span></span><b>导航</b>
    </button>

    <aside class="orbit-rail" :class="{ open: mobileOpen }">
      <button class="orbit-brand" type="button" aria-label="返回首页" @click="go('/')">
        <span class="brand-logo"><img src="/resume-logo.svg" alt="履历星图" /></span>
        <span><strong>履历星图</strong><small>RESUME ORBIT</small></span>
      </button>

      <nav class="planet-nav" aria-label="主导航">
        <span class="orbit-line" aria-hidden="true"></span>
        <button
          v-for="item in navItems"
          :key="item.path"
          type="button"
          :class="{ active: active(item.path) }"
          :style="{ '--planet-hue': item.hue }"
          @click="go(item.path)"
        >
          <span class="planet-glyph" aria-hidden="true">{{ item.glyph }}</span>
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="rail-footer">
        <ThemeSwitcher />
        <button class="identity-chip" type="button" @click="handleIdentity">
          <span class="identity-avatar">{{ identity.slice(0, 1) }}</span>
          <span><strong>{{ identity }}</strong><small>{{ userStore.isLoggedIn || adminStore.isLoggedIn ? '点击退出当前会话' : '登录以同步进度' }}</small></span>
        </button>
      </div>
    </aside>

    <section class="orbit-stage">
      <header class="stage-header">
        <div class="breadcrumb"><span>履历星图</span><b>/</b><strong>{{ route.meta.title || navItems.find(i => active(i.path))?.label || '未知区域' }}</strong></div>
        <div class="stage-status"><i></i><span>云端服务正常</span><b>{{ userStore.isLoggedIn ? `AI ${userStore.totalAiQuota ?? 0} 次` : '本地探索模式' }}</b></div>
      </header>

      <main class="stage-content">
        <router-view v-slot="{ Component, route: currentRoute }">
          <transition name="sector-shift">
            <div :key="currentRoute.fullPath" class="route-scene">
              <component :is="Component" />
            </div>
          </transition>
        </router-view>
      </main>

      <footer class="stage-footer">
        <span>© {{ year }} 履历星图 · 由 <a class="footer-author" href="https://mine-resume.lcode.space/" target="_blank" rel="noopener noreferrer">lcode</a> 打造</span>
        <div><button @click="openLegal('terms')">服务协议</button><button @click="openLegal('privacy')">隐私政策</button><button @click="openLegal('membership')">会员说明</button></div>
      </footer>
    </section>

    <button v-if="mobileOpen" class="rail-scrim" type="button" aria-label="关闭导航" @click="mobileOpen = false"></button>
    <LegalDialog v-model:visible="legalVisible" :doc="legalDoc" />
    <AnnouncementDialog />
  </div>
</template>
