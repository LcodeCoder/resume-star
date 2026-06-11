<!--
  模板库页面
  功能：按分类筛选简历模板，卡片渲染模板真实缩略图（迷你 A4 预览），
        悬停浮出"使用此模板"按钮，一键套用后跳转编辑器；会员模板展示标识（不拦截）
  设计参考：Canva 简历模板页——胶囊分类筛选 + 缩略图卡片网格
-->
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { applyTemplate } from '../api/resume'
import { listTemplateCategories, listTemplates, toggleTemplateFavorite } from '../api/template'
import { listMemberPackages } from '../api/member'
import { getUserSystemConfig } from '../api/user'
import { useUserStore } from '../store/user'
import TemplatePreview from '../components/template-preview/TemplatePreview.vue'
import MemberUpgradeDialog from '../components/member-tip/MemberUpgradeDialog.vue'

const router = useRouter()
const userStore = useUserStore()
const categories = ref([])
const templates = ref([])
const activeCategory = ref('')
const packages = ref([])
const visible = ref(false)
const systemConfig = ref({ paymentEnabled: false, mockPaymentEnabled: true })

/** 兼容历史模板数据：补齐简历页面级样式字段 */
const ensureResumeStyle = (resume) => ({
  ...resume,
  style: {
    background: '#ffffff',
    ...(resume?.style || {})
  }
})

const loadTemplates = async () => {
  templates.value = (await listTemplates({
    categoryCode: activeCategory.value,
    userId: userStore.profile?.id || 1
  })).map(ensureResumeStyle)
}

/** 切换分类并刷新列表 */
const switchCategory = async (code) => {
  activeCategory.value = code
  await loadTemplates()
}

onMounted(async () => {
  categories.value = await listTemplateCategories()
  await userStore.loadProfile()
  const [packageList, config] = await Promise.all([
    listMemberPackages(),
    getUserSystemConfig()
  ])
  packages.value = packageList
  systemConfig.value = config || systemConfig.value
  await loadTemplates()
})

const isVipUser = () => userStore.profile?.vipLevel && userStore.profile.vipLevel !== 'FREE'

/**
 * 套用模板
 * 作用：调用后端创建一份基于模板的简历草稿，并跳转编辑器
 */
const useTemplate = async (template) => {
  if (template.vipTemplate && !isVipUser()) {
    visible.value = true
    return
  }
  await applyTemplate(template.id, { userId: userStore.profile?.id || 1 })
  router.push('/editor')
}

/**
 * 收藏 / 取消收藏模板
 * 作用：切换收藏状态并就地更新卡片上的收藏标识与收藏数，无需整列表刷新
 */
const toggleFavorite = async (template) => {
  const favorited = await toggleTemplateFavorite(template.id, { userId: userStore.profile?.id || 1 })
  template.favorited = favorited
  template.favoriteCount = Math.max(0, (template.favoriteCount || 0) + (favorited ? 1 : -1))
  ElMessage.success(favorited ? '已加入收藏' : '已取消收藏')
}

</script>

<template>
  <section class="page-header card">
    <h2>简历模板库</h2>
    <p>挑选一套喜欢的模板，一键套用到编辑器继续创作。</p>
    <div class="chip-row">
      <button class="chip" :class="{ active: activeCategory === '' }" @click="switchCategory('')">全部</button>
      <button
        v-for="item in categories"
        :key="item.id"
        class="chip"
        :class="{ active: activeCategory === item.code }"
        @click="switchCategory(item.code)"
      >
        {{ item.name }} {{ item.count }}
      </button>
    </div>
  </section>

  <section class="template-grid">
    <article v-for="item in templates" :key="item.id" class="template-card card">
      <!-- 模板缩略图：按组件数据等比渲染，悬停浮出套用按钮 -->
      <div class="tpl-cover" :class="{ locked: item.vipTemplate && !isVipUser() }">
        <TemplatePreview :components="item.components" :page-style="item.style" size="medium" />
        <!-- 收藏按钮：常驻右上角，已收藏高亮实心 -->
        <button
          class="tpl-fav-btn"
          :class="{ active: item.favorited }"
          :title="item.favorited ? '取消收藏' : '收藏模板'"
          @click.stop="toggleFavorite(item)"
        >{{ item.favorited ? '♥' : '♡' }}</button>
        <!-- 会员模板对非会员显示锁标识 -->
        <span v-if="item.vipTemplate && !isVipUser()" class="tpl-lock-badge">🔒 会员专属</span>
        <div class="tpl-overlay">
          <el-button type="primary" @click="useTemplate(item)">
            {{ item.vipTemplate && !isVipUser() ? '升级会员解锁' : '使用此模板' }}
          </el-button>
        </div>
      </div>
      <div class="template-meta">
        <h3>{{ item.name }} <span v-if="item.vipTemplate" class="vip-badge">会员</span></h3>
        <p>{{ item.styleTag }}｜收藏 {{ item.favoriteCount }}｜浏览 {{ item.viewCount }}</p>
      </div>
    </article>
  </section>

  <MemberUpgradeDialog
    v-model:visible="visible"
    :packages="packages"
    :payment-enabled="systemConfig.paymentEnabled !== false"
    :shop-url="systemConfig.shopUrl"
  />
</template>
