<!--
  个人中心页面
  功能：展示用户资料、会员等级、剩余额度；并以标签页形式管理「全部简历 / 草稿箱 / 我的收藏 / 操作记录」
-->
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'
import { listResumes, listDraftResumes, publishResume, deleteResume, applyTemplate } from '../api/resume'
import { listFavoriteTemplates } from '../api/template'
import { changeMyPassword, updateMyProfile, listMyActivities, clearMyActivities } from '../api/user'
import { submitCase, submitArticle, deleteCaseByResume, listMyArticles, deleteArticle } from '../api/community'
import { listQuotaLedger } from '../api/member'
import request from '../api/request'
import TemplatePreview from '../components/template-preview/TemplatePreview.vue'

const router = useRouter()
const userStore = useUserStore()
const resumes = ref([])
const drafts = ref([])
const favorites = ref([])
const activities = ref([])
const quotaLedger = ref([])
const myLikes = ref({ cases: [], articles: [] })
const myArticles = ref([])
/** 当前激活的标签页：resumes-全部简历 drafts-草稿箱 favorites-我的收藏 likes-我的点赞 articles-我的技巧 activity-操作记录 ledger-额度流水 */
const activeTab = ref('resumes')

const profile = computed(() => userStore.profile || {})

/** 无头像时用昵称 / 账号首字母占位 */
const initial = computed(() => {
  const name = profile.value.nickname || profile.value.username || '?'
  return name.trim().charAt(0).toUpperCase()
})

const isVip = computed(() => !!userStore.vipLevel)

/** 会员到期时间格式化为「YYYY-MM-DD HH:mm」 */
const expireText = computed(() => {
  const t = profile.value.vipExpireTime
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
})

/** 操作记录类型 → 展示图标，前端按类型渲染不同色块 */
const ACTIVITY_ICONS = {
  LOGIN: '🔑', REGISTER: '✨', SAVE: '💾', PUBLISH: '🚀',
  EXPORT: '⤓', AI: 'AI', FAVORITE: '♥', TEMPLATE: '🎨', SECURITY: '🔒', OTHER: '·'
}
const activityIcon = (type) => ACTIVITY_ICONS[type] || ACTIVITY_ICONS.OTHER
/** 格式化操作时间为「MM-DD HH:mm」 */
const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(5, 16) : '')

/* ===== 账号设置 ===== */
const settingsVisible = ref(false)
const savingProfile = ref(false)
const savingPassword = ref(false)
const profileForm = reactive({ nickname: '', email: '', avatar: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const openSettings = () => {
  profileForm.nickname = profile.value.nickname || ''
  profileForm.email = profile.value.email || ''
  profileForm.avatar = profile.value.avatar || ''
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  settingsVisible.value = true
}

const handleAvatarChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  // 与编辑器头像一致：读为 base64 内联存储，免依赖文件服务
  const reader = new FileReader()
  reader.onload = () => {
    profileForm.avatar = reader.result
    ElMessage.success('头像已选择，保存后生效')
  }
  reader.readAsDataURL(file)
  event.target.value = ''
}

const saveProfile = async () => {
  savingProfile.value = true
  try {
    const updated = await updateMyProfile({
      nickname: profileForm.nickname,
      email: profileForm.email,
      avatar: profileForm.avatar
    })
    if (updated) {
      userStore.profile = { ...userStore.profile, ...updated }
      ElMessage.success('资料已更新')
    }
  } finally {
    savingProfile.value = false
  }
}

const savePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写原密码和新密码')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPassword.value = true
  try {
    await changeMyPassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } finally {
    savingPassword.value = false
  }
}

/** 打开指定简历进入编辑器 */
const openResume = (id) => router.push({ path: '/editor', query: id ? { id } : {} })

/** 当前用户 ID（演示环境兜底为 1） */
const currentUserId = () => profile.value.id || 1

/** 加载全部简历、草稿箱、收藏、操作记录、额度流水 */
const loadAll = async () => {
  const userId = currentUserId()
  const [all, draftList, favList, actList, likes, articleList, ledger] = await Promise.all([
    listResumes({ userId }),
    listDraftResumes({ userId }),
    listFavoriteTemplates({ userId }),
    listMyActivities(),
    request.get(`/community/my-likes?userId=${userId}`),
    listMyArticles(userId).catch(() => []),
    listQuotaLedger(userId).catch(() => [])
  ])
  // 获取已投稿的案例
  const cases = await request.get('/community/cases').catch(() => [])
  const submittedIds = new Set(cases.filter(c => c.authorId === userId).map(c => {
    const match = c.resumeData?.match(/^resumeId:(\d+)/)
    return match ? parseInt(match[1]) : null
  }).filter(Boolean))

  resumes.value = (all || []).map(r => ({ ...r, submitted: submittedIds.has(r.id) }))
  drafts.value = draftList || []
  favorites.value = favList || []
  activities.value = actList || []
  quotaLedger.value = ledger || []
  myLikes.value = likes || { cases: [], articles: [] }
  myArticles.value = articleList || []
}

/** 发布草稿为正式简历 */
const handlePublish = async (item) => {
  await publishResume(item.id, { userId: currentUserId() })
  // 立即更新本地状态
  item.draft = false
  ElMessage.success('已发布为正式简历')
  await loadAll()
}

/** 删除简历（草稿或正式） */
const handleDelete = async (item) => {
  await ElMessageBox.confirm(`确定删除「${item.title || '未命名简历'}」吗？`, '删除确认', { type: 'warning' })
  await deleteResume(item.id, { userId: currentUserId() })
  await deleteCaseByResume(item.id).catch(() => {}) // 同时删除社区投稿
  ElMessage.success('已删除')
  await loadAll()
}

const submitDialogVisible = ref(false)
const submitForm = reactive({ resumeId: null, title: '', description: '', tags: '' })

const handleSubmitCase = (item) => {
  submitForm.resumeId = item.id
  submitForm.title = item.title
  submitForm.description = ''
  submitForm.tags = ''
  submitDialogVisible.value = true
}

const confirmSubmit = async () => {
  if (!submitForm.description) {
    ElMessage.warning('请填写简历描述')
    return
  }
  await submitCase({ ...submitForm, userId: currentUserId() })
  submitDialogVisible.value = false
  ElMessage.success('投稿成功！内容可展示 1 小时，管理员审核通过后将长期展示')
  await loadAll()
}

const handleWithdrawCase = async (item) => {
  await ElMessageBox.confirm('确定撤销投稿吗？', '撤销确认', { type: 'warning' })
  await deleteCaseByResume(item.id)
  ElMessage.success('已撤销投稿')
  await loadAll()
}

const articleDialogVisible = ref(false)
const articleForm = reactive({ title: '', summary: '', content: '', category: '技巧分享' })

const openArticleSubmit = () => {
  articleForm.title = ''
  articleForm.summary = ''
  articleForm.content = ''
  articleForm.category = '技巧分享'
  articleDialogVisible.value = true
}

const confirmArticleSubmit = async () => {
  if (!articleForm.title || !articleForm.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  await submitArticle({ ...articleForm, authorId: currentUserId() })
  articleDialogVisible.value = false
  ElMessage.success('投稿成功！管理员审核通过后将长期展示')
  await loadAll()
}

/** 撤回 / 删除我投稿的技巧文章 */
const handleDeleteArticle = async (item) => {
  await ElMessageBox.confirm(`确定删除技巧「${item.title}」吗？`, '删除确认', { type: 'warning' })
  await deleteArticle(item.id)
  ElMessage.success('已删除')
  await loadAll()
}

/** 清空操作记录 */
const handleClearActivities = async () => {
  await ElMessageBox.confirm('确定清空全部操作记录吗？', '清空确认', { type: 'warning' })
  await clearMyActivities()
  ElMessage.success('已清空')
  await loadAll()
}

/** 套用收藏的模板并进入编辑器 */
const useFavorite = async (template) => {
  await applyTemplate(template.id, { userId: currentUserId() })
  router.push('/editor')
}

onMounted(async () => {
  await userStore.loadProfile()
  await loadAll()
})
</script>

<template>
  <section class="profile-page">
    <div class="card profile-hero">
      <div class="profile-hero-main">
        <div class="profile-avatar-lg">
          <img v-if="profile.avatar" :src="profile.avatar" alt="头像" />
          <span v-else>{{ initial }}</span>
        </div>
        <div class="profile-identity">
          <h2 class="profile-name">{{ profile.nickname || profile.username || '未登录' }}</h2>
          <p class="profile-handle">@{{ profile.username }}</p>
          <div class="profile-vip-row">
            <span v-if="isVip" class="vip-badge">{{ userStore.vipLevelLabel }}</span>
            <span v-else class="profile-chip-free">{{ userStore.vipLevelLabel }}</span>
            <span v-if="isVip && expireText" class="profile-meta">有效期至 {{ expireText }}</span>
            <span v-if="profile.email" class="profile-meta">{{ profile.email }}</span>
          </div>
        </div>
      </div>
      <div class="profile-actions">
        <el-button @click="openSettings">账号设置</el-button>
        <el-button @click="router.push('/editor')">新建简历</el-button>
        <el-button @click="openArticleSubmit">投稿技巧</el-button>
        <el-button type="primary" @click="router.push('/member')">{{ isVip ? '续费会员' : '升级会员' }}</el-button>
      </div>
    </div>

    <div class="profile-quota-grid">
      <article class="card profile-quota-card tone-ai">
        <span class="profile-quota-icon">AI</span>
        <div>
          <strong>{{ userStore.remainingAiQuota }}</strong>
          <p>今日 AI 剩余次数<template v-if="userStore.quota && !userStore.quota.aiUnlimited">（上限 {{ userStore.quota.aiLimit }}）</template></p>
        </div>
      </article>
      <article class="card profile-quota-card tone-export">
        <span class="profile-quota-icon">⤓</span>
        <div>
          <strong>{{ userStore.remainingExportQuota }}</strong>
          <p>今日导出剩余次数<template v-if="userStore.quota && !userStore.quota.exportUnlimited">（上限 {{ userStore.quota.exportLimit }}）</template></p>
        </div>
      </article>
      <article class="card profile-quota-card tone-vip">
        <span class="profile-quota-icon">★</span>
        <div>
          <strong>{{ userStore.vipLevelLabel }}</strong>
          <p>{{ isVip && expireText ? '有效期至 ' + expireText : '当前会员等级' }}</p>
        </div>
      </article>
    </div>

    <div class="card profile-resumes">
      <el-tabs v-model="activeTab" class="profile-tabs">
        <!-- 全部简历 -->
        <el-tab-pane name="resumes">
          <template #label>全部简历 <span class="tab-count">{{ resumes.length }}</span></template>
          <div v-if="resumes.length" class="resume-list">
            <div v-for="item in resumes" :key="item.id" class="resume-item">
              <button class="resume-item-open" @click="openResume(item.id)">
                <div class="resume-item-main">
                  <span class="resume-item-title">
                    {{ item.title }}
                    <span class="resume-tag" :class="item.draft ? 'tag-draft' : 'tag-published'">{{ item.draft ? '草稿' : '正式' }}</span>
                  </span>
                  <span class="resume-item-job">{{ item.targetJob || '未设置目标岗位' }}</span>
                </div>
              </button>
              <div class="resume-item-ops">
                <el-button v-if="item.draft" size="small" type="primary" plain @click="handlePublish(item)">发布</el-button>
                <el-button v-if="!item.draft && !item.submitted" size="small" @click="handleSubmitCase(item)">投稿到社区</el-button>
                <el-button v-if="!item.draft && item.submitted" size="small" type="danger" plain @click="handleWithdrawCase(item)">撤销投稿</el-button>
                <el-button size="small" type="danger" plain @click="handleDelete(item)">删除</el-button>
              </div>
            </div>
          </div>
          <div v-else class="resume-empty">
            <p>还没有简历，开始创建你的第一份简历吧</p>
            <el-button type="primary" @click="router.push('/editor')">立即创建</el-button>
          </div>
        </el-tab-pane>

        <!-- 草稿箱 -->
        <el-tab-pane name="drafts">
          <template #label>草稿箱 <span class="tab-count">{{ drafts.length }}</span></template>
          <div v-if="drafts.length" class="resume-list">
            <div v-for="item in drafts" :key="item.id" class="resume-item">
              <button class="resume-item-open" @click="openResume(item.id)">
                <div class="resume-item-main">
                  <span class="resume-item-title">
                    {{ item.title }}
                    <span class="resume-tag tag-draft">草稿</span>
                  </span>
                  <span class="resume-item-job">{{ item.targetJob || '未设置目标岗位' }}</span>
                </div>
              </button>
              <div class="resume-item-ops">
                <el-button size="small" type="primary" plain @click="handlePublish(item)">发布</el-button>
                <el-button size="small" type="danger" plain @click="handleDelete(item)">删除</el-button>
              </div>
            </div>
          </div>
          <div v-else class="resume-empty">
            <p>草稿箱是空的，在编辑器中保存的草稿会出现在这里</p>
            <el-button type="primary" @click="router.push('/editor')">去编辑器</el-button>
          </div>
        </el-tab-pane>

        <!-- 我的收藏 -->
        <el-tab-pane name="favorites">
          <template #label>我的收藏 <span class="tab-count">{{ favorites.length }}</span></template>
          <div v-if="favorites.length" class="fav-grid">
            <article v-for="tpl in favorites" :key="tpl.id" class="fav-card">
              <div class="fav-cover">
                <TemplatePreview :components="tpl.components" :page-style="tpl.style" size="compact" />
                <div class="fav-overlay">
                  <el-button type="primary" size="small" @click="useFavorite(tpl)">使用此模板</el-button>
                </div>
              </div>
              <div class="fav-meta">
                <span>{{ tpl.name }}</span>
                <span v-if="tpl.vipTemplate" class="vip-badge">会员</span>
              </div>
            </article>
          </div>
          <div v-else class="resume-empty">
            <p>还没有收藏模板，去模板库挑一套喜欢的吧</p>
            <el-button type="primary" @click="router.push('/templates')">浏览模板库</el-button>
          </div>
        </el-tab-pane>

        <!-- 我的技巧 -->
        <el-tab-pane name="articles">
          <template #label>我的技巧 <span class="tab-count">{{ myArticles.length }}</span></template>
          <div v-if="myArticles.length" class="resume-list">
            <div v-for="item in myArticles" :key="item.id" class="resume-item">
              <div class="resume-item-open as-static">
                <div class="resume-item-main">
                  <span class="resume-item-title">
                    {{ item.title }}
                    <span class="resume-tag" :class="item.published ? 'tag-published' : 'tag-draft'">
                      {{ item.published ? '已通过' : '待审核' }}
                    </span>
                  </span>
                  <span class="resume-item-job">
                    {{ item.category || '技巧分享' }} · <el-icon><User /></el-icon> {{ item.viewCount }} · <el-icon><Pointer /></el-icon> {{ item.likeCount }}
                  </span>
                </div>
              </div>
              <div class="resume-item-ops">
                <el-button size="small" type="danger" plain @click="handleDeleteArticle(item)">删除</el-button>
              </div>
            </div>
          </div>
          <div v-else class="resume-empty">
            <p>还没有投稿优化技巧，分享你的简历优化经验吧</p>
            <el-button type="primary" @click="openArticleSubmit">投稿技巧</el-button>
          </div>
        </el-tab-pane>

        <!-- 我的点赞 -->
        <el-tab-pane name="likes">
          <template #label>我的点赞 <span class="tab-count">{{ myLikes.cases.length + myLikes.articles.length }}</span></template>
          <div v-if="myLikes.cases.length || myLikes.articles.length">
            <div v-if="myLikes.cases.length" class="likes-section">
              <h3>简历案例</h3>
              <div class="likes-grid">
                <div v-for="item in myLikes.cases" :key="item.id" class="like-card" @click="router.push('/community')">
                  <h4>{{ item.title }}</h4>
                  <p>{{ item.description }}</p>
                  <div class="like-meta">
                    <span><el-icon><User /></el-icon> {{ item.viewCount }}</span>
                    <span><el-icon><Pointer /></el-icon> {{ item.likeCount }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="myLikes.articles.length" class="likes-section">
              <h3>优化技巧</h3>
              <div class="likes-grid">
                <div v-for="item in myLikes.articles" :key="item.id" class="like-card" @click="router.push('/community')">
                  <h4>{{ item.title }}</h4>
                  <p>{{ item.summary }}</p>
                  <div class="like-meta">
                    <span><el-icon><User /></el-icon> {{ item.viewCount }}</span>
                    <span>{{ item.category }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="resume-empty">
            <p>还没有点赞内容，去社区看看吧</p>
            <el-button type="primary" @click="router.push('/community')">访问社区</el-button>
          </div>
        </el-tab-pane>

        <!-- 操作记录 -->
        <el-tab-pane name="activity">
          <template #label>操作记录 <span class="tab-count">{{ activities.length }}</span></template>
          <div v-if="activities.length">
            <div style="margin-bottom: 16px; text-align: right;">
              <el-button type="danger" size="small" plain @click="handleClearActivities">清空记录</el-button>
            </div>
            <ul class="activity-list">
              <li v-for="log in activities" :key="log.id" class="activity-item">
                <span class="activity-icon" :class="'act-' + (log.type || 'OTHER').toLowerCase()">{{ activityIcon(log.type) }}</span>
                <span class="activity-action">{{ log.action }}</span>
                <span class="activity-time">{{ formatTime(log.createTime) }}</span>
              </li>
            </ul>
          </div>
          <div v-else class="resume-empty">
            <p>暂无操作记录</p>
          </div>
        </el-tab-pane>

        <!-- 额度流水：充值余额的兑换充入与消耗明细 -->
        <el-tab-pane name="ledger">
          <template #label>额度流水 <span class="tab-count">{{ quotaLedger.length }}</span></template>
          <el-table v-if="quotaLedger.length" :data="quotaLedger" stripe>
            <el-table-column label="时间" width="130">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="说明" min-width="170">
              <template #default="{ row }">{{ row.action }}</template>
            </el-table-column>
            <el-table-column label="AI 次数" width="100" align="center">
              <template #default="{ row }">
                <span v-if="row.aiChange" :class="row.aiChange > 0 ? 'ledger-in' : 'ledger-out'">{{ row.aiChange > 0 ? '+' + row.aiChange : row.aiChange }}</span>
                <span v-else class="ledger-none">—</span>
              </template>
            </el-table-column>
            <el-table-column label="导出次数" width="100" align="center">
              <template #default="{ row }">
                <span v-if="row.exportChange" :class="row.exportChange > 0 ? 'ledger-in' : 'ledger-out'">{{ row.exportChange > 0 ? '+' + row.exportChange : row.exportChange }}</span>
                <span v-else class="ledger-none">—</span>
              </template>
            </el-table-column>
            <el-table-column label="变动后余额" width="160" align="center">
              <template #default="{ row }">
                <span class="ledger-balance">AI {{ row.aiBalanceAfter }} / 导出 {{ row.exportBalanceAfter }}</span>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="resume-empty">
            <p>暂无额度流水，使用额度兑换码充值次数后这里会展示明细</p>
            <el-button type="primary" @click="router.push('/member')">前往会员中心</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 账号设置弹窗：资料编辑 + 修改密码 -->
    <el-dialog v-model="settingsVisible" title="账号设置" width="460px">
      <el-form label-position="top">
        <el-form-item label="头像">
          <div class="settings-avatar-row">
            <div class="profile-avatar-lg settings-avatar">
              <img v-if="profileForm.avatar" :src="profileForm.avatar" alt="头像" />
              <span v-else>{{ initial }}</span>
            </div>
            <label class="avatar-upload-button">
              更换头像
              <input type="file" accept="image/*" @change="handleAvatarChange" />
            </label>
          </div>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="20" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱（选填）" />
        </el-form-item>
        <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
      </el-form>

      <el-divider />

      <el-form label-position="top">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
        <el-button :loading="savingPassword" @click="savePassword">修改密码</el-button>
      </el-form>

      <template #footer>
        <el-button @click="settingsVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 投稿对话框 -->
    <el-dialog v-model="submitDialogVisible" width="540px" align-center class="submit-dialog" :show-close="true">
      <template #header>
        <div class="submit-dialog-header">
          <span class="submit-dialog-icon">🚀</span>
          <div>
            <h3 class="submit-dialog-title">投稿到社区</h3>
            <p class="submit-dialog-subtitle">分享你的优秀简历，帮助更多求职者</p>
          </div>
        </div>
      </template>
      <el-form label-position="top" class="submit-form">
        <el-form-item label="简历标题">
          <el-input v-model="submitForm.title" placeholder="例如：全栈工程师简历 - 突出项目量化成果" />
        </el-form-item>
        <el-form-item label="简历描述">
          <el-input v-model="submitForm.description" type="textarea" :rows="3" placeholder="简要介绍这份简历的亮点、适用岗位或特色..." />
        </el-form-item>
        <el-form-item label="标签（逗号分隔）">
          <el-input v-model="submitForm.tags" placeholder="例如：全栈,5年经验,互联网" />
        </el-form-item>
        <div class="submit-tips">
          <span class="submit-tips-icon">🔒</span>
          <div class="submit-tips-text">
            <p>投稿后系统会自动<strong>脱敏处理</strong>（姓名、电话、邮箱替换为 *****）</p>
            <p>投稿内容可展示 <strong>1 小时</strong>，管理员审核通过后将长期展示</p>
          </div>
        </div>
      </el-form>
      <template #footer>
        <div class="submit-dialog-footer">
          <el-button @click="submitDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmSubmit">提交投稿</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 文章投稿对话框 -->
    <el-dialog v-model="articleDialogVisible" width="700px" align-center class="submit-dialog" :show-close="true">
      <template #header>
        <div class="submit-dialog-header">
          <span class="submit-dialog-icon">✍️</span>
          <div>
            <h3 class="submit-dialog-title">投稿优化技巧</h3>
            <p class="submit-dialog-subtitle">分享你的简历优化经验与心得</p>
          </div>
        </div>
      </template>
      <el-form label-position="top" class="submit-form">
        <el-form-item label="文章标题">
          <el-input v-model="articleForm.title" placeholder="例如：如何用 STAR 法则优化项目经历" />
        </el-form-item>
        <el-form-item label="文章摘要">
          <el-input v-model="articleForm.summary" type="textarea" :rows="2" placeholder="一句话概括文章主要内容..." />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="articleForm.category" placeholder="选择分类" style="width: 100%">
            <el-option label="技巧分享" value="技巧分享" />
            <el-option label="经验总结" value="经验总结" />
            <el-option label="行业观察" value="行业观察" />
          </el-select>
        </el-form-item>
        <el-form-item label="文章内容（支持 Markdown）">
          <el-input v-model="articleForm.content" type="textarea" :rows="12" placeholder="支持 Markdown 语法：&#10;# 一级标题&#10;## 二级标题&#10;**加粗文字**&#10;- 列表项" />
        </el-form-item>
        <div class="submit-tips">
          <span class="submit-tips-icon">⏱️</span>
          <div class="submit-tips-text">
            <p>投稿内容可展示 <strong>1 小时</strong>，管理员审核通过后将长期展示</p>
          </div>
        </div>
      </el-form>
      <template #footer>
        <div class="submit-dialog-footer">
          <el-button @click="articleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmArticleSubmit">提交投稿</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.likes-section {
  margin-bottom: 24px;
}

.likes-section h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 16px;
  color: #303133;
}

.likes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.like-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.like-card:hover {
  border-color: #5b5bd6;
  box-shadow: 0 2px 12px rgba(91, 91, 214, 0.15);
}

.like-card h4 {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 8px;
  color: #303133;
}

.like-card p {
  font-size: 13px;
  color: #606266;
  margin: 0 0 12px;
  line-height: 1.6;
}

.like-meta {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #909399;
}

/* 投稿对话框美化 */
:deep(.submit-dialog) {
  border-radius: 18px;
  overflow: hidden;
}

:deep(.submit-dialog .el-dialog__header) {
  margin: 0;
  padding: 24px 28px 18px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

:deep(.submit-dialog .el-dialog__body) {
  padding: 22px 28px 4px;
}

:deep(.submit-dialog .el-dialog__footer) {
  padding: 16px 28px 24px;
}

.submit-dialog-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.submit-dialog-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 13px;
  background: linear-gradient(135deg, #e8f2ff, #e8e8fa);
  font-size: 22px;
  flex-shrink: 0;
}

.submit-dialog-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1f;
}

.submit-dialog-subtitle {
  margin: 3px 0 0;
  font-size: 13px;
  color: #909399;
}

.submit-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #1d1d1f;
  padding-bottom: 6px;
}

.submit-form :deep(.el-input__wrapper),
.submit-form :deep(.el-textarea__inner) {
  border-radius: 10px;
}

.submit-tips {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 4px;
  background: #f5f9ff;
  border: 1px solid #e8e8fa;
  border-radius: 12px;
}

.submit-tips-icon {
  font-size: 18px;
  line-height: 1.4;
  flex-shrink: 0;
}

.submit-tips-text p {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: #6e6e73;
}

.submit-tips-text strong {
  color: #5b5bd6;
  font-weight: 600;
}

.submit-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.submit-dialog-footer .el-button {
  min-width: 92px;
  border-radius: 10px;
}

/* 我的技巧列表项：纯展示，不需要按钮态的指针与悬停背景 */
.resume-item-open.as-static {
  cursor: default;
}

.resume-item-open.as-static:hover {
  background: #fff;
}
</style>
