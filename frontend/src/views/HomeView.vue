<!--
  首页视图
  功能：展示产品定位、核心能力入口、平台数据和热门模板预览
  设计参考：Apple / Linear 风格——双栏主视觉（文案 + 悬浮简历预览）、滚动渐入动效、
            数字滚动统计、能力卡片、热门模板与底部行动号召区
-->
<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listTemplates, listTemplateCategories } from '../api/template'
import { getSiteStats } from '../api/stats'
import { flattenComponents } from '../data/componentLibrary'
import TemplatePreview from '../components/template-preview/TemplatePreview.vue'

const router = useRouter()
const hotTemplates = ref([])
/** 主视觉右侧悬浮展示：在多套模板间自动轮播，形成"动态 demo" */
const heroTemplates = ref([])
const heroIndex = ref(0)
let heroTimer = null

const features = [
  {
    title: '可视化简历编辑器',
    desc: '组件化画布，拖拽排版、样式自定义，草稿自动保存。',
    icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>`,
    color: '#5b5bd6',
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

/** 平台统计：均为真实数据——组件数取自前端组件库定义，其余在挂载时由接口校准后滚动展示 */
const stats = reactive([
  { target: flattenComponents().length, suffix: '+', label: '内置组件', display: 0 },
  { target: 0, suffix: '', label: '精选模板', display: 0 },
  { target: 0, suffix: '', label: '行业分类', display: 0 },
  { target: 0, suffix: '+', label: 'AI 优化次数', display: 0 }
])

/** 是否偏好减少动效（无障碍） */
const reduceMotion = typeof window !== 'undefined'
  && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

/**
 * 滚动渐入指令：元素进入视口后添加 is-visible 类触发过渡
 * 用法 v-reveal 或 v-reveal="延迟毫秒"，对应 transition-delay
 */
const vReveal = {
  mounted(el, binding) {
    if (reduceMotion) {
      el.classList.add('is-visible')
      return
    }
    el.classList.add('reveal')
    if (binding.value) el.style.transitionDelay = `${binding.value}ms`
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          el.classList.add('is-visible')
          observer.unobserve(el)
        }
      })
    }, { threshold: 0.15 })
    observer.observe(el)
  }
}

/** 数字滚动动画：在统计区进入视口时把 display 从 0 推进到 target */
const runCountUp = () => {
  const numeric = stats.filter((s) => typeof s.target === 'number')
  if (reduceMotion) {
    numeric.forEach((s) => { s.display = s.target })
    return
  }
  const duration = 1100
  const start = performance.now()
  const tick = (now) => {
    const progress = Math.min((now - start) / duration, 1)
    // easeOutCubic，结尾减速更自然
    const eased = 1 - Math.pow(1 - progress, 3)
    numeric.forEach((s) => { s.display = Math.round(s.target * eased) })
    if (progress < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

let countStarted = false
/** 统计条进入视口时触发一次数字滚动 */
const vCountup = {
  mounted(el) {
    if (reduceMotion) { runCountUp(); return }
    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && !countStarted) {
        countStarted = true
        runCountUp()
        observer.unobserve(el)
      }
    }, { threshold: 0.4 })
    observer.observe(el)
  }
}

/** 兼容历史模板数据：补齐简历页面级样式字段 */
const ensureResumeStyle = (resume) => ({
  ...resume,
  style: {
    background: '#ffffff',
    ...(resume?.style || {})
  }
})

onMounted(async () => {
  const [templates, categories, siteStats] = await Promise.all([
    listTemplates({ categoryCode: '' }),
    listTemplateCategories().catch(() => []),
    getSiteStats().catch(() => null)
  ])
  const list = templates.map(ensureResumeStyle)
  hotTemplates.value = list.slice(0, 4)
  heroTemplates.value = list.slice(0, 5)
  startHeroRotate()
  // 接口返回后校准统计：精选模板 / 行业分类 / 累计 AI 优化次数
  stats[1].target = templates.length || stats[1].target
  stats[2].target = (categories || []).length || stats[2].target
  if (siteStats?.aiCallCount != null) stats[3].target = Number(siteStats.aiCallCount)
  // 如果数字滚动已经播放完，直接把展示值同步到最新目标
  if (countStarted) {
    stats.forEach((s) => { s.display = s.target })
  }
})

/** 跳转编辑器（需要时可在此加引导逻辑） */
const startEditing = () => router.push('/editor')

/** 主视觉模板自动轮播（减少动效偏好下不启动） */
const startHeroRotate = () => {
  if (reduceMotion || heroTemplates.value.length <= 1) return
  heroTimer = setInterval(() => {
    heroIndex.value = (heroIndex.value + 1) % heroTemplates.value.length
  }, 3500)
}

/** 手动切换展示模板，并停止自动轮播（尊重用户操作） */
const selectHero = (i) => {
  heroIndex.value = i
  if (heroTimer) {
    clearInterval(heroTimer)
    heroTimer = null
  }
}

onBeforeUnmount(() => {
  if (heroTimer) clearInterval(heroTimer)
})
</script>

<template>
  <!-- 主视觉：双栏（文案 + 悬浮简历预览）+ 动态渐变背景 -->
  <section class="hero-section">
    <!-- 装饰性背景元素 -->
    <div class="hero-decor" aria-hidden="true">
      <div class="hero-circle hero-circle-1"></div>
      <div class="hero-circle hero-circle-2"></div>
      <div class="hero-circle hero-circle-3"></div>
      <div class="hero-grid-overlay"></div>
    </div>

    <div class="hero-inner">
      <div class="hero-content">
        <div class="hero-badge"><span class="hero-badge-dot"></span>智能简历平台</div>
        <h1>用 <span class="hero-gradient-text">AI</span> 打造<br/>更专业的中文简历</h1>
        <p>resume-lcode 集简历编辑、模板套用、AI 润色与一键导出于一体，几分钟做出一份拿得出手的简历。</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round @click="startEditing">
            开始编辑简历
          </el-button>
          <el-button size="large" round @click="router.push('/templates')">
            浏览模板库
          </el-button>
        </div>
        <div class="hero-trust">
          <span class="hero-trust-item">✓ 免费使用</span>
          <span class="hero-trust-item">✓ 数据云端保存</span>
          <span class="hero-trust-item">✓ 一键导出 PDF</span>
        </div>
      </div>

      <!-- 悬浮简历预览：所有模板一次性渲染并常驻（缓存），仅用透明度交叉淡入，
           避免切换时重新挂载导致"还没加载出来就跳到下一张"的闪烁 -->
      <div class="hero-visual">
        <div class="hero-preview-stack">
          <div
            v-for="(t, i) in heroTemplates"
            :key="t.id"
            class="hero-preview-card"
            :class="{ active: i === heroIndex }"
            :aria-hidden="i !== heroIndex"
          >
            <TemplatePreview :components="t.components" :page-style="t.style" size="medium" />
          </div>
        </div>
        <div class="hero-chip hero-chip-ai">AI 润色 +28%</div>
        <div class="hero-chip hero-chip-export">⤓ 一键导出</div>
        <div v-if="heroTemplates.length > 1" class="hero-dots">
          <button
            v-for="(t, i) in heroTemplates"
            :key="t.id"
            class="hero-dot"
            :class="{ active: i === heroIndex }"
            :aria-label="`查看第 ${i + 1} 套模板`"
            @click="selectHero(i)"
          ></button>
        </div>
      </div>
    </div>
  </section>

  <!-- 统计数据条：数字滚动 -->
  <section v-countup class="stats-bar" v-reveal>
    <div v-for="item in stats" :key="item.label" class="stat-item">
      <span class="stat-value">
        <template v-if="item.text">{{ item.text }}</template>
        <template v-else>{{ item.display }}{{ item.suffix }}</template>
      </span>
      <span class="stat-label">{{ item.label }}</span>
    </div>
  </section>

  <!-- 核心能力：带图标的卡片 -->
  <section class="feature-section">
    <div class="section-header" v-reveal>
      <div>
        <h2>核心能力</h2>
        <p>一站式完成简历制作、优化与导出的全部流程</p>
      </div>
    </div>
    <div class="feature-grid">
      <article
        v-for="(item, idx) in features"
        :key="item.title"
        class="feature-card card"
        v-reveal="idx * 90"
      >
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
    <div class="section-header" v-reveal>
      <h2>热门模板</h2>
      <el-button text type="primary" @click="router.push('/templates')">查看全部 →</el-button>
    </div>
    <div class="hot-template-grid">
      <article
        v-for="(item, idx) in hotTemplates"
        :key="item.id"
        class="hot-template-card card"
        v-reveal="idx * 90"
        @click="router.push('/templates')"
      >
        <div class="hot-template-cover">
          <TemplatePreview :components="item.components" :page-style="item.style" size="compact" />
          <div class="hot-template-overlay"><span>使用模板</span></div>
        </div>
        <div class="panel-template-name">
          <span>{{ item.name }}</span>
          <span v-if="item.vipTemplate" class="vip-badge">会员</span>
        </div>
      </article>
    </div>
  </section>

  <!-- 底部行动号召区 -->
  <section class="cta-band" v-reveal>
    <div class="cta-decor" aria-hidden="true"></div>
    <div class="cta-content">
      <h2>准备好做一份更好的简历了吗？</h2>
      <p>现在就开始，几分钟内完成一份专业简历。</p>
      <el-button type="primary" size="large" round @click="startEditing">立即免费开始</el-button>
    </div>
  </section>
</template>
