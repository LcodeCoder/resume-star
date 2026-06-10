<!--
  首页视图
  功能：展示产品定位、核心能力入口、平台数据和热门模板预览
  设计参考：Apple / Linear 风格——大标题渐变背景 + 特性图标卡片 + 统计区 + 模板网格
-->
<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listTemplates } from '../api/template'
import TemplatePreview from '../components/template-preview/TemplatePreview.vue'

const router = useRouter()
const hotTemplates = ref([])

const features = [
  {
    title: '可视化简历编辑器',
    desc: '组件化画布，拖拽排版、样式自定义，草稿自动保存。',
    icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>`,
    color: '#0071e3',
    bg: '#eef5ff'
  },
  {
    title: '多行业模板库',
    desc: '覆盖技术、产品、校招等场景，挑选后一键套用。',
    icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"/><path d="m22 6-10 7L2 6"/></svg>`,
    color: '#8b5cf6',
    bg: '#f3f0ff'
  },
  {
    title: 'AI 智能优化',
    desc: '一键润色、纠错与岗位适配，让简历更有竞争力。',
    icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a4 4 0 0 0-4 4v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2h-2V6a4 4 0 0 0-4-4z"/><circle cx="12" cy="15" r="2"/></svg>`,
    color: '#f59e0b',
    bg: '#fef9ee'
  },
  {
    title: '云端存储与导出',
    desc: '简历云端保存，支持导出 PDF 与图片，随时取用。',
    icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>`,
    color: '#10b981',
    bg: '#ecfdf5'
  }
]

const stats = [
  { value: '48+', label: '内置组件' },
  { value: '10+', label: '简历模板' },
  { value: '5', label: '行业分类' },
  { value: '∞', label: 'AI 优化次数' }
]

/** 兼容历史模板数据：补齐简历页面级样式字段 */
const ensureResumeStyle = (resume) => ({
  ...resume,
  style: {
    background: '#ffffff',
    ...(resume?.style || {})
  }
})

onMounted(async () => {
  const templates = await listTemplates({ categoryCode: '' })
  hotTemplates.value = templates.slice(0, 4).map(ensureResumeStyle)
})
</script>

<template>
  <!-- 主视觉：渐变背景 + 大标题 + 双按钮 -->
  <section class="hero-section">
    <div class="hero-content">
      <div class="hero-badge">智能简历平台</div>
      <h1>用 AI 打造<br/>更专业的中文简历</h1>
      <p>resume-lcode 集简历编辑、模板套用、AI 润色与一键导出于一体，几分钟做出一份拿得出手的简历。</p>
      <div class="hero-actions">
        <el-button type="primary" size="large" round @click="router.push('/editor')">
          开始编辑简历
        </el-button>
        <el-button size="large" round @click="router.push('/templates')">
          浏览模板库
        </el-button>
      </div>
    </div>
    <!-- 装饰性背景元素 -->
    <div class="hero-decor">
      <div class="hero-circle hero-circle-1"></div>
      <div class="hero-circle hero-circle-2"></div>
      <div class="hero-circle hero-circle-3"></div>
    </div>
  </section>

  <!-- 统计数据条 -->
  <section class="stats-bar">
    <div v-for="item in stats" :key="item.label" class="stat-item">
      <span class="stat-value">{{ item.value }}</span>
      <span class="stat-label">{{ item.label }}</span>
    </div>
  </section>

  <!-- 核心能力：带图标的卡片 -->
  <section class="feature-section">
    <div class="section-header">
      <h2>核心能力</h2>
      <p>一站式完成简历制作、优化与导出的全部流程</p>
    </div>
    <div class="feature-grid">
      <article v-for="item in features" :key="item.title" class="feature-card card">
        <div class="feature-icon" :style="{ background: item.bg, color: item.color }">
          <span v-html="item.icon"></span>
        </div>
        <h3>{{ item.title }}</h3>
        <p>{{ item.desc }}</p>
      </article>
    </div>
  </section>

  <!-- 热门模板 -->
  <section class="home-templates">
    <div class="section-header">
      <h2>热门模板</h2>
      <el-button text type="primary" @click="router.push('/templates')">查看全部 →</el-button>
    </div>
    <div class="hot-template-grid">
      <article
        v-for="item in hotTemplates"
        :key="item.id"
        class="hot-template-card card"
        @click="router.push('/templates')"
      >
        <div class="hot-template-cover">
          <TemplatePreview :components="item.components" :page-style="item.style" size="compact" />
        </div>
        <div class="panel-template-name">
          <span>{{ item.name }}</span>
          <span v-if="item.vipTemplate" class="vip-badge">会员</span>
        </div>
      </article>
    </div>
  </section>
</template>
