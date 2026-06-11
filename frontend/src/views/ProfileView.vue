<!--
  个人中心页面
  功能：展示用户资料、会员等级、剩余额度和我的简历列表
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { listResumes } from '../api/resume'

const router = useRouter()
const userStore = useUserStore()
const resumes = ref([])

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

onMounted(async () => {
  await userStore.loadProfile()
  resumes.value = await listResumes({ userId: profile.value.id || 1 })
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
        <el-button @click="router.push('/editor')">新建简历</el-button>
        <el-button type="primary" @click="router.push('/member')">{{ isVip ? '续费会员' : '升级会员' }}</el-button>
      </div>
    </div>

    <div class="profile-quota-grid">
      <article class="card profile-quota-card tone-ai">
        <span class="profile-quota-icon">AI</span>
        <div>
          <strong>{{ userStore.remainingAiQuota }}</strong>
          <p>AI 优化剩余次数</p>
        </div>
      </article>
      <article class="card profile-quota-card tone-export">
        <span class="profile-quota-icon">⤓</span>
        <div>
          <strong>{{ userStore.remainingExportQuota }}</strong>
          <p>导出剩余次数</p>
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
      <div class="profile-resumes-head">
        <div>
          <h3>我的简历</h3>
          <p>共 {{ resumes.length }} 份简历</p>
        </div>
        <el-button text type="primary" @click="router.push('/editor')">进入编辑器 →</el-button>
      </div>

      <div v-if="resumes.length" class="resume-list">
        <button
          v-for="item in resumes"
          :key="item.id"
          class="resume-item"
          @click="router.push('/editor')"
        >
          <div class="resume-item-main">
            <span class="resume-item-title">{{ item.title }}</span>
            <span class="resume-item-job">{{ item.targetJob || '未设置目标岗位' }}</span>
          </div>
          <span class="resume-item-arrow">→</span>
        </button>
      </div>

      <div v-else class="resume-empty">
        <p>还没有简历，开始创建你的第一份简历吧</p>
        <el-button type="primary" @click="router.push('/editor')">立即创建</el-button>
      </div>
    </div>
  </section>
</template>
