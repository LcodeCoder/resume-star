<!--
  后台管理容器页
  功能：承载统计概览、用户管理、VIP 配置、模板管理、模拟测试、AI 配置和系统配置模块
-->
<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminDashboardTab from './admin/AdminDashboardTab.vue'
import AdminUsersTab from './admin/AdminUsersTab.vue'
import AdminMembersTab from './admin/AdminMembersTab.vue'
import AdminVipConfigTab from './admin/AdminVipConfigTab.vue'
import AdminTemplatesTab from './admin/AdminTemplatesTab.vue'
import AdminOrdersTab from './admin/AdminOrdersTab.vue'
import AdminAuditTab from './admin/AdminAuditTab.vue'
import AdminAiTab from './admin/AdminAiTab.vue'
import AdminAiLogsTab from './admin/AdminAiLogsTab.vue'
import AdminExportRecordsTab from './admin/AdminExportRecordsTab.vue'
import AdminInterviewTab from './admin/AdminInterviewTab.vue'
import AdminAnnouncementTab from './admin/AdminAnnouncementTab.vue'
import AdminCommunityTab from './admin/AdminCommunityTab.vue'
import AdminSystemTab from './admin/AdminSystemTab.vue'
import AdminAccountTab from './admin/AdminAccountTab.vue'

const route = useRoute()
const router = useRouter()

const tabs = [
  { key: 'dashboard', label: '统计概览', component: AdminDashboardTab },
  { key: 'users', label: '用户管理', component: AdminUsersTab },
  { key: 'members', label: '会员管理', component: AdminMembersTab },
  { key: 'vip', label: 'VIP 配置', component: AdminVipConfigTab },
  { key: 'templates', label: '模板管理', component: AdminTemplatesTab },
  { key: 'interview', label: '模拟测试', component: AdminInterviewTab },
  { key: 'orders', label: '订单营收', component: AdminOrdersTab },
  { key: 'audit', label: '操作日志', component: AdminAuditTab },
  { key: 'ai', label: 'AI 配置', component: AdminAiTab },
  { key: 'ai-logs', label: 'AI 调用日志', component: AdminAiLogsTab },
  { key: 'export-records', label: '导出记录', component: AdminExportRecordsTab },
  { key: 'announcement', label: '公告管理', component: AdminAnnouncementTab },
  { key: 'community', label: '社区管理', component: AdminCommunityTab },
  { key: 'system', label: '系统配置', component: AdminSystemTab },
  { key: 'account', label: '账号设置', component: AdminAccountTab }
]

const tabKeys = tabs.map((t) => t.key)

const resolveTab = (key) => (tabKeys.includes(key) ? key : 'dashboard')

const activeTab = ref(resolveTab(route.query.tab))

watch(
  () => route.query.tab,
  (key) => {
    if (key && tabKeys.includes(key)) activeTab.value = key
  }
)

const onTabClick = (key) => {
  activeTab.value = key
  // 同步到 URL，便于刷新保持 Tab、从编辑器返回时定位到该 Tab
  router.replace({ path: '/admin', query: key === 'dashboard' ? {} : { tab: key } })
}

const currentTab = computed(() => tabs.find((item) => item.key === activeTab.value)?.component || AdminDashboardTab)
</script>

<template>
  <section class="page-header card admin-page-header">
    <div>
      <h2>后台管理</h2>
      <p>集中管理平台数据、用户权益、模板权限、模拟面试与 AI 配置。</p>
    </div>
    <div class="admin-tabs">
      <button
        v-for="item in tabs"
        :key="item.key"
        class="admin-tab"
        :class="{ active: activeTab === item.key }"
        @click="onTabClick(item.key)"
      >
        {{ item.label }}
      </button>
    </div>
  </section>

  <component :is="currentTab" />
</template>
