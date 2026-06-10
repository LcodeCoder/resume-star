<!--
  个人中心页面
  功能：展示用户资料、会员等级、剩余额度和我的简历列表
-->
<script setup>
import { onMounted, ref } from 'vue'
import { useUserStore } from '../store/user'
import { listResumes } from '../api/resume'

const userStore = useUserStore()
const resumes = ref([])

onMounted(async () => {
  await userStore.loadProfile()
  resumes.value = await listResumes({ userId: 1 })
})
</script>

<template>
  <section class="profile-grid">
    <div class="card profile-card">
      <h2>{{ userStore.profile?.nickname }}</h2>
      <p>账号：{{ userStore.profile?.username }}</p>
      <p>会员等级：{{ userStore.vipLevelLabel }}</p>
      <p>AI 剩余额度：{{ userStore.profile?.remainingAiQuota }}</p>
      <p>导出剩余额度：{{ userStore.profile?.remainingExportQuota }}</p>
    </div>
    <div class="card profile-card">
      <h2>我的简历</h2>
      <div v-for="item in resumes" :key="item.id" class="list-row">
        <span>{{ item.title }}</span>
        <span>{{ item.targetJob }}</span>
      </div>
    </div>
  </section>
</template>
