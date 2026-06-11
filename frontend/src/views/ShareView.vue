<!--
  简历公开分享页（只读）
  功能：通过分享 token 拉取简历内容，以 A4 缩略渲染展示，无需登录
-->
<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import TemplatePreview from '../components/template-preview/TemplatePreview.vue'
import { viewResumeShare } from '../api/resume'

const route = useRoute()
const share = ref(null)
const loading = ref(true)
const notFound = ref(false)

onMounted(async () => {
  try {
    const data = await viewResumeShare(route.params.token)
    if (data) share.value = data
    else notFound.value = true
  } catch (e) {
    notFound.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="share-page">
    <header class="share-page-header">
      <div class="brand">
        <img class="brand-logo" src="/resume-logo.png" alt="resume-lcode" />
        <div>
          <div class="brand-title">resume-lcode</div>
          <div class="brand-subtitle">在线简历分享</div>
        </div>
      </div>
      <a class="auth-link" href="/">制作我的简历 →</a>
    </header>

    <main class="share-page-body" v-loading="loading">
      <div v-if="share" class="share-resume-card card">
        <div class="share-resume-head">
          <div>
            <h2>{{ share.title }}</h2>
            <p class="muted">{{ share.targetJob || '在线简历' }}</p>
          </div>
          <span class="share-views muted">浏览 {{ share.viewCount ?? 0 }}</span>
        </div>
        <div class="share-resume-paper">
          <TemplatePreview :components="share.components" :page-style="share.style" size="large" />
        </div>
      </div>

      <el-empty v-else-if="notFound && !loading" description="分享不存在或已失效" />
    </main>
  </div>
</template>
