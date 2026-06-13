/**
 * 前端路由配置
 * 功能：注册首页、编辑器、模板库、个人中心、后台管理和登录页；含登录守卫
 */
import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'
import HomeView from '../views/HomeView.vue'
import EditorView from '../views/EditorView.vue'
import TemplatesView from '../views/TemplatesView.vue'
import ProfileView from '../views/ProfileView.vue'
import AdminView from '../views/AdminView.vue'
import MemberView from '../views/MemberView.vue'
import CommunityView from '../views/CommunityView.vue'
import InterviewView from '../views/InterviewView.vue'
import LoginView from '../views/LoginView.vue'
import ShareView from '../views/ShareView.vue'
import { useUserStore } from '../store/user'
import { useAdminStore } from '../store/admin'

/** 需要用户登录的路由 */
const USER_GUARDED = ['editor', 'profile', 'member', 'interview']
/** 需要管理员登录的路由 */
const ADMIN_GUARDED = ['admin']

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior() {
    return { top: 0 }
  },
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/admin/login', redirect: (to) => ({ path: '/login', query: { ...to.query, role: 'admin' } }) },
    { path: '/s/:token', name: 'share', component: ShareView, meta: { public: true } },
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', name: 'home', component: HomeView, meta: { public: true } },
        { path: 'editor', name: 'editor', component: EditorView },
        { path: 'templates', name: 'templates', component: TemplatesView, meta: { public: true } },
        { path: 'community', name: 'community', component: CommunityView, meta: { public: true } },
        { path: 'profile', name: 'profile', component: ProfileView },
        { path: 'admin', name: 'admin', component: AdminView },
        { path: 'member', name: 'member', component: MemberView },
        { path: 'interview', name: 'interview', component: InterviewView }
      ]
    }
  ]
})

router.beforeEach(async (to, from, next) => {
  // 公开页直接放行
  if (to.meta.public) return next()

  // 管理员编辑模板模式，跳过登录检查
  if (to.name === 'editor' && to.query.adminMode === 'true') {
    const adminStore = useAdminStore()
    if (!adminStore.isLoggedIn) {
      await adminStore.loadProfile()
    }
    if (!adminStore.isLoggedIn) {
      return next({ path: '/login', query: { role: 'admin', redirect: to.fullPath } })
    }
    return next()
  }

  // 管理员守卫
  if (ADMIN_GUARDED.includes(to.name)) {
    const adminStore = useAdminStore()
    if (!adminStore.isLoggedIn) {
      await adminStore.loadProfile()
    }
    if (!adminStore.isLoggedIn) {
      return next({ path: '/login', query: { role: 'admin', redirect: to.fullPath } })
    }
    return next()
  }

  // 用户守卫
  if (USER_GUARDED.includes(to.name)) {
    const userStore = useUserStore()
    if (!userStore.authReady) {
      await userStore.loadProfile()
    }
    if (!userStore.isLoggedIn) {
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }
    return next()
  }

  next()
})

export default router

