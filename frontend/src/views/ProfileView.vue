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
import { changeMyPassword, updateMyProfile, listMyActivities } from '../api/user'
import { submitCase, submitArticle, deleteCaseByResume } from '../api/community'
import TemplatePreview from '../components/template-preview/TemplatePreview.vue'

const router = useRouter()
const userStore = useUserStore()
const resumes = ref([])
const drafts = ref([])
const favorites = ref([])
const activities = ref([])
/** 当前激活的标签页：resumes-全部简历 drafts-草稿箱 favorites-我的收藏 activity-操作记录 */
const activeTab = ref('resumes')

const profile = computed(() => userStore.profile || {})

/** 无头像时用昵称 / 账号首字母占位 */
const initial = computed(() => {
  const name = profile.value.nickname || profile.value.username || '?'
  return name.trim().charAt(0).toUpperCase()
})

const isVip = computed(() => (userStore.vipLevel || 'FREE') !== 'FREE')

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

/** 加载全部简历、草稿箱、收藏、操作记录 */
const loadAll = async () => {
  const userId = currentUserId()
  const [all, draftList, favList, actList] = await Promise.all([
    listResumes({ userId }),
    listDraftResumes({ userId }),
    listFavoriteTemplates({ userId }),
    listMyActivities()
  ])
  resumes.value = all || []
  drafts.value = draftList || []
  favorites.value = favList || []
  activities.value = actList || []
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
  await submitArticle(articleForm)
  articleDialogVisible.value = false
  ElMessage.success('投稿成功！内容可展示 1 小时，管理员审核通过后将长期展示')
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
                <el-button v-if="!item.draft" size="small" @click="handleSubmitCase(item)">投稿到社区</el-button>
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

        <!-- 操作记录 -->
        <el-tab-pane name="activity">
          <template #label>操作记录 <span class="tab-count">{{ activities.length }}</span></template>
          <ul v-if="activities.length" class="activity-list">
            <li v-for="log in activities" :key="log.id" class="activity-item">
              <span class="activity-icon" :class="'act-' + (log.type || 'OTHER').toLowerCase()">{{ activityIcon(log.type) }}</span>
              <span class="activity-action">{{ log.action }}</span>
              <span class="activity-time">{{ formatTime(log.createTime) }}</span>
            </li>
          </ul>
          <div v-else class="resume-empty">
            <p>暂无操作记录</p>
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
    <el-dialog v-model="submitDialogVisible" title="投稿到社区" width="520px">
      <el-form label-position="top">
        <el-form-item label="简历标题">
          <el-input v-model="submitForm.title" placeholder="例如：全栈工程师简历 - 突出项目量化成果" />
        </el-form-item>
        <el-form-item label="简历描述">
          <el-input v-model="submitForm.description" type="textarea" :rows="3" placeholder="简要介绍这份简历的亮点、适用岗位或特色..." />
        </el-form-item>
        <el-form-item label="标签（逗号分隔）">
          <el-input v-model="submitForm.tags" placeholder="例如：全栈,5年经验,互联网" />
        </el-form-item>
        <el-alert type="info" :closable="false" style="margin-bottom: 16px">
          投稿后系统会自动脱敏（姓名、电话、邮箱替换为 *****），投稿内容可展示 1 小时，管理员审核通过后将长期展示
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="submitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSubmit">提交投稿</el-button>
      </template>
    </el-dialog>

    <!-- 文章投稿对话框 -->
    <el-dialog v-model="articleDialogVisible" title="投稿优化技巧" width="680px">
      <el-form label-position="top">
        <el-form-item label="文章标题">
          <el-input v-model="articleForm.title" placeholder="例如：如何用 STAR 法则优化项目经历" />
        </el-form-item>
        <el-form-item label="文章摘要">
          <el-input v-model="articleForm.summary" type="textarea" :rows="2" placeholder="一句话概括文章主要内容..." />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="articleForm.category" placeholder="选择分类">
            <el-option label="技巧分享" value="技巧分享" />
            <el-option label="经验总结" value="经验总结" />
            <el-option label="行业观察" value="行业观察" />
          </el-select>
        </el-form-item>
        <el-form-item label="文章内容（支持 Markdown）">
          <el-input v-model="articleForm.content" type="textarea" :rows="12" placeholder="支持 Markdown 语法：&#10;# 一级标题&#10;## 二级标题&#10;**加粗文字**&#10;- 列表项" />
        </el-form-item>
        <el-alert type="info" :closable="false" style="margin-bottom: 16px">
          投稿内容可展示 1 小时，管理员审核通过后将长期展示
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="articleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmArticleSubmit">提交投稿</el-button>
      </template>
    </el-dialog>
  </section>
</template>
