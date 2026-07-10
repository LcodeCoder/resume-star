<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { listTemplates } from '../api/template'
import { getSiteStats } from '../api/stats'

const router = useRouter()
const userStore = useUserStore()
const templateCount = ref('—')
const aiCount = ref('—')
const greeting = computed(() => {
  const hour = new Date().getHours()
  const phase = hour < 6 ? '夜深了' : hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好'
  return `${phase}，${userStore.profile?.nickname || '探索者'}`
})

const missions = [
  { route: '/editor', code: 'CORE', title: '建立主简历', text: '从经历、教育和技能开始，形成可复用的母版。', action: '进入简历工坊', status: '建议先完成' },
  { route: '/templates', code: 'STYLE', title: '选择投递外观', text: '按行业和岗位切换版式，内容无需重复填写。', action: '探索模板星库', status: '随时可切换' },
  { route: '/interview', code: 'VOICE', title: '校准面试表达', text: '用真实问题练习，让简历上的每一句都有例证。', action: '启动模拟面试', status: '准备阶段' }
]

onMounted(async () => {
  const [templates, stats] = await Promise.all([
    listTemplates({ categoryCode: '' }).catch(() => []),
    getSiteStats().catch(() => null)
  ])
  templateCount.value = templates.length || 0
  aiCount.value = stats?.aiCallCount != null ? Number(stats.aiCallCount).toLocaleString('zh-CN') : '持续增长'
})
</script>

<template>
  <div class="mission-home">
    <section class="mission-intro">
      <div class="intro-copy">
        <span class="signal"><i></i> 工作区已就绪</span>
        <h1>{{ greeting }}<br>今天把履历推进一段。</h1>
        <p>从一份可信的主简历出发，再针对岗位调整表达。星图会记录你的内容、版本与练习进度。</p>
        <div class="intro-actions">
          <el-button type="primary" size="large" @click="router.push('/editor')">继续编辑简历</el-button>
          <button class="text-action" type="button" @click="router.push('/templates')">先看模板 <span>→</span></button>
        </div>
      </div>

      <div class="orbit-map" aria-label="求职准备轨道图">
        <span class="map-axis axis-x"></span><span class="map-axis axis-y"></span>
        <div class="central-sun"><b>你</b><small>当前坐标</small></div>
        <button class="map-planet planet-resume" @click="router.push('/editor')"><i></i><span>简历</span><small>内容核心</small></button>
        <button class="map-planet planet-template" @click="router.push('/templates')"><i></i><span>模板</span><small>{{ templateCount }} 个可用</small></button>
        <button class="map-planet planet-interview" @click="router.push('/interview')"><i></i><span>面试</span><small>表达训练</small></button>
        <div class="orbit-ring ring-one"></div><div class="orbit-ring ring-two"></div>
      </div>
    </section>

    <section class="mission-board">
      <header><div><h2>当前航程</h2><p>按成果推进，不必一次完成所有事情。</p></div><span>{{ userStore.isLoggedIn ? '进度已同步' : '登录后可跨设备同步' }}</span></header>
      <div class="mission-list">
        <article v-for="(mission, index) in missions" :key="mission.code">
          <div class="mission-index"><b>{{ String(index + 1).padStart(2, '0') }}</b><span>{{ mission.code }}</span></div>
          <div class="mission-body"><div class="mission-title"><h3>{{ mission.title }}</h3><span>{{ mission.status }}</span></div><p>{{ mission.text }}</p></div>
          <button type="button" @click="router.push(mission.route)">{{ mission.action }} <span>↗</span></button>
        </article>
      </div>
    </section>

    <section class="observatory-strip">
      <div><span>模板库</span><strong>{{ templateCount }}</strong><small>套可切换版式</small></div>
      <div><span>AI 润色</span><strong>{{ aiCount }}</strong><small>次平台累计调用</small></div>
      <blockquote>“简历不是经历的仓库，而是一条指向下一站的清晰航线。”</blockquote>
      <button type="button" @click="router.push('/community')">查看社区案例</button>
    </section>
  </div>
</template>

<style scoped>
.mission-home { display: grid; gap: 42px; }
.mission-intro { min-height: 520px; display: grid; grid-template-columns: minmax(0, .9fr) minmax(430px, 1.1fr); align-items: center; gap: 4vw; padding: 46px 0 28px; border-bottom: 1px solid var(--line); }
.intro-copy { max-width: 620px; }.signal { display: inline-flex; align-items: center; gap: 8px; margin-bottom: 24px; color: var(--ink-2); font-size: 12px; }.signal i { width: 8px; height: 8px; border-radius: 50%; background: var(--success); box-shadow: 0 0 0 4px color-mix(in oklch, var(--success), transparent 82%); }
h1 { margin: 0; max-width: 12ch; font-size: 46px; line-height: 1.12; letter-spacing: -.04em; } .intro-copy > p { max-width: 55ch; margin: 22px 0 0; color: var(--ink-2); font-size: 16px; line-height: 1.8; }
.intro-actions { display: flex; align-items: center; gap: 22px; margin-top: 32px; }.text-action { border: 0; background: transparent; color: var(--ink); font-weight: 700; cursor: pointer; }.text-action span { color: var(--accent); }
.orbit-map { position: relative; min-height: 450px; overflow: hidden; }.orbit-ring { position: absolute; inset: 50% auto auto 50%; border: 1px solid var(--line); border-radius: 50%; transform: translate(-50%, -50%) rotate(-12deg); }.ring-one { width: 260px; height: 140px; }.ring-two { width: 430px; height: 260px; transform: translate(-50%, -50%) rotate(14deg); }.map-axis { position: absolute; background: color-mix(in oklch, var(--line), transparent 40%); }.axis-x { width: 100%; height: 1px; top: 50%; }.axis-y { width: 1px; height: 100%; left: 50%; }.central-sun { position: absolute; z-index: 3; inset: 50% auto auto 50%; width: 104px; height: 104px; display: flex; align-items: center; justify-content: center; flex-direction: column; border-radius: 50%; background: var(--star); color: oklch(.2 .04 70); transform: translate(-50%, -50%); box-shadow: 0 0 0 14px color-mix(in oklch, var(--star), transparent 78%), 0 0 38px color-mix(in oklch, var(--star), transparent 42%); }.central-sun b { font-size: 22px; }.central-sun small { font-size: 10px; }
.map-planet { position: absolute; z-index: 4; display: grid; grid-template-columns: 24px 1fr; column-gap: 8px; align-items: center; padding: 9px 12px; border: 1px solid var(--line); border-radius: var(--radius-md); background: var(--surface); box-shadow: var(--shadow); text-align: left; cursor: pointer; transition: transform 180ms cubic-bezier(.16,1,.3,1), border-color 180ms ease; }.map-planet:hover { border-color: var(--accent); transform: translateY(-3px); }.map-planet i { grid-row: 1 / 3; width: 18px; height: 18px; border-radius: 50%; background: var(--accent); }.map-planet span { font-weight: 800; }.map-planet small { color: var(--muted); font-size: 10px; }.planet-resume { left: 5%; top: 19%; }.planet-template { right: 2%; top: 23%; }.planet-template i { background: var(--success); }.planet-interview { right: 12%; bottom: 13%; }.planet-interview i { background: var(--warning); }
.mission-board header { display: flex; justify-content: space-between; align-items: end; gap: 20px; margin-bottom: 8px; }.mission-board h2 { margin: 0; font-size: 24px; }.mission-board header p { margin: 4px 0 0; color: var(--muted); }.mission-board header > span { color: var(--muted); font-size: 11px; }
.mission-list article { display: grid; grid-template-columns: 80px minmax(0, 1fr) auto; align-items: center; gap: 22px; min-height: 116px; border-bottom: 1px solid var(--line); }.mission-index { display: flex; flex-direction: column; }.mission-index b { font-size: 21px; }.mission-index span { color: var(--muted); font: 700 9px/1.4 ui-monospace, monospace; letter-spacing: .1em; }.mission-title { display: flex; align-items: center; gap: 10px; }.mission-title h3 { margin: 0; font-size: 17px; }.mission-title span { padding: 3px 7px; border-radius: var(--radius-pill); background: var(--surface-2); color: var(--muted); font-size: 10px; }.mission-body p { margin: 6px 0 0; color: var(--ink-2); }.mission-list article > button { padding: 9px 0 9px 14px; border: 0; background: transparent; color: var(--ink); font-weight: 700; cursor: pointer; }.mission-list article > button span { color: var(--accent); }
.observatory-strip { display: grid; grid-template-columns: .6fr .8fr 1.5fr auto; align-items: center; gap: 28px; padding: 26px 0; border-top: 1px solid var(--line); border-bottom: 1px solid var(--line); }.observatory-strip > div { display: grid; }.observatory-strip span, .observatory-strip small { color: var(--muted); font-size: 10px; }.observatory-strip strong { margin: 2px 0; font-size: 22px; }.observatory-strip blockquote { margin: 0; padding-left: 24px; border-left: 1px solid var(--line); color: var(--ink-2); }.observatory-strip button { padding: 9px 12px; border: 1px solid var(--line); border-radius: var(--radius-sm); background: transparent; cursor: pointer; }
@media (max-width: 1050px) { .mission-intro { grid-template-columns: 1fr; }.orbit-map { min-height: 390px; }.observatory-strip { grid-template-columns: 1fr 1fr; }.observatory-strip blockquote { grid-column: 1 / -1; } }
@media (max-width: 640px) { .mission-intro { min-height: 0; padding-top: 22px; } h1 { font-size: 34px; }.orbit-map { min-height: 340px; margin-inline: -12px; }.ring-two { width: 330px; }.map-planet { padding: 7px 8px; }.planet-resume { left: 0; }.planet-template { right: 0; }.mission-list article { grid-template-columns: 48px 1fr; gap: 12px; padding: 18px 0; }.mission-list article > button { grid-column: 2; justify-self: start; padding-left: 0; }.mission-title { align-items: flex-start; flex-direction: column; gap: 4px; }.observatory-strip { grid-template-columns: 1fr 1fr; gap: 20px; }.observatory-strip button { grid-column: 1 / -1; } }
</style>
