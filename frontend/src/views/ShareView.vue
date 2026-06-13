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
    <div class="share-bg-decor" aria-hidden="true"></div>
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
          <span class="share-views muted">👁 浏览 {{ share.viewCount ?? 0 }}</span>
        </div>
        <div class="share-resume-paper">
          <TemplatePreview :components="share.components" :page-style="share.style" size="large" />
        </div>
        <footer class="share-resume-footer">
          <span>由 <strong>resume-lcode</strong> 在线简历平台生成</span>
          <a href="/">免费制作你的简历 →</a>
        </footer>
      </div>

      <el-empty v-else-if="notFound && !loading" description="分享不存在或已失效" />
    </main>
  </div>
</template>

<style scoped>
.share-page {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(180deg, #f7faff 0%, #ecf2fb 100%);
  padding-bottom: 60px;
  overflow: hidden;
}
.share-bg-decor {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 10% 0%, rgba(37, 99, 235, 0.12) 0%, transparent 40%),
    radial-gradient(circle at 90% 30%, rgba(16, 185, 129, 0.1) 0%, transparent 40%);
  pointer-events: none;
}
.share-page-header {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 40px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}
.brand { display: flex; align-items: center; gap: 12px; }
.brand-logo {
  width: 36px; height: 36px; border-radius: 10px;
  box-shadow: 0 6px 14px rgba(37,99,235,0.2);
}
.brand-title { font-size: 16px; font-weight: 700; color: #0f172a; }
.brand-subtitle { font-size: 12px; color: #64748b; margin-top: 2px; }
.auth-link {
  padding: 8px 16px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  font-size: 14px;
  border-radius: 999px;
  text-decoration: none;
  box-shadow: 0 8px 22px rgba(37,99,235,0.3);
  transition: 0.2s;
}
.auth-link:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 26px rgba(37,99,235,0.36);
}
.share-page-body {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  padding: 40px 24px;
}
.share-resume-card {
  width: 100%;
  max-width: 880px;
  padding: 28px;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 30px 80px rgba(15,23,42,0.1);
}
.share-resume-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 22px;
  padding-bottom: 22px;
  border-bottom: 1px dashed rgba(15,23,42,0.1);
}
.share-resume-head h2 { margin: 0 0 6px; font-size: 22px; color: #0f172a; }
.muted { color: #64748b; font-size: 13px; }
.share-views {
  padding: 6px 12px;
  background: rgba(37,99,235,0.08);
  color: #2563eb;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}
.share-resume-paper {
  background: linear-gradient(180deg, #f8fafc, #ffffff);
  border-radius: 14px;
  padding: 24px;
  border: 1px solid rgba(15,23,42,0.04);
}
.share-resume-footer {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px dashed rgba(15,23,42,0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #64748b;
}
.share-resume-footer strong { color: #2563eb; }
.share-resume-footer a {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
}
.share-resume-footer a:hover { text-decoration: underline; }
@media (max-width: 720px) {
  .share-page-header { padding: 14px 18px; }
  .share-resume-card { padding: 18px; border-radius: 16px; }
  .share-page-body { padding: 22px 12px; }
}
</style>
