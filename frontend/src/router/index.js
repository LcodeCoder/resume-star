/**
 * 前端路由配置
 * 功能：注册首页、编辑器、模板库、个人中心、后台管理和登录页；含登录守卫
 */
import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'
import { useUserStore } from '../store/user'
import { useAdminStore } from '../store/admin'
import { beginNav, endNav } from '../utils/globalLoader'

// 视图按路由懒加载：每个页面单独拆成 chunk，首屏只下载入口 + 布局 + 首页，
// 其余页面在进入对应路由时按需加载，显著降低首包体积。
const HomeView = () => import('../views/HomeView.vue')
const EditorView = () => import('../views/EditorView.vue')
const TemplatesView = () => import('../views/TemplatesView.vue')
const ProfileView = () => import('../views/ProfileView.vue')
const AdminView = () => import('../views/AdminView.vue')
const MemberView = () => import('../views/MemberView.vue')
const CommunityView = () => import('../views/CommunityView.vue')
const InterviewView = () => import('../views/InterviewView.vue')
const LoginView = () => import('../views/LoginView.vue')
const ShareView = () => import('../views/ShareView.vue')

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
  // 进入新页面：进入「导航阶段」（顶部进度条 + 长任务 Lcode 遮罩）。
  // 用布尔阶段而非计数，重定向(next 跳转)再多次也不会泄漏遮罩。
  if (from.name !== to.name || from.fullPath !== to.fullPath) {
    beginNav()
  }
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

/** 路由切换结束：结束「导航中」标记，遮罩会延续到首屏数据加载完成 */
router.afterEach(() => {
  endNav()
})

/** 异常或被重定向中断：兜底结束导航，避免遮罩一直挂着 */
router.onError(() => {
  endNav()
})

export default router

