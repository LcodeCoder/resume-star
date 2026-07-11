<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import ThemeSwitcher from '../components/common/ThemeSwitcher.vue'

const STORAGE_KEY = 'orbit_resume_prefs'

const loadPrefs = () => {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
  } catch (_) {
    return {}
  }
}

const saved = loadPrefs()
const autosave = ref(saved.autosave !== false)
const compact = ref(!!saved.compact)
const motion = ref(saved.motion !== false && !window.matchMedia?.('(prefers-reduced-motion: reduce)').matches)
const emailNotice = ref(saved.emailNotice !== false)
const interviewNotice = ref(!!saved.interviewNotice)

const applyPrefs = () => {
  document.documentElement.dataset.density = compact.value ? 'compact' : 'comfortable'
  document.documentElement.dataset.motion = motion.value ? 'full' : 'reduce'
  document.documentElement.dataset.autosave = autosave.value ? 'on' : 'off'
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      autosave: autosave.value,
      compact: compact.value,
      motion: motion.value,
      emailNotice: emailNotice.value,
      interviewNotice: interviewNotice.value
    }))
  } catch (_) { /* storage may be disabled */ }
}

const toastApplied = () => ElMessage.success('已应用到本设备')

watch([autosave, compact, motion, emailNotice, interviewNotice], () => {
  applyPrefs()
  toastApplied()
})

onMounted(applyPrefs)
</script>

<template>
  <div class="settings-page">
    <header class="page-header">
      <div>
        <h1>系统设置</h1>
        <p>调整阅读环境、编辑习惯与通知方式。更改会立即保存到本设备。</p>
      </div>
    </header>
    <div class="settings-layout">
      <nav aria-label="设置分类">
        <a href="#appearance">显示环境</a>
        <a href="#editing">编辑习惯</a>
        <a href="#notice">通知</a>
        <a href="#privacy">隐私与数据</a>
      </nav>
      <div class="settings-sections">
        <section id="appearance">
          <header><span>显示环境</span><p>四套主题拥有各自的对比度与语义色，不是简单滤镜。</p></header>
          <div class="setting-row">
            <div><strong>界面主题</strong><small>可随时切换，选择会保存在当前设备。</small></div>
            <ThemeSwitcher />
          </div>
          <div class="setting-row">
            <div><strong>动效</strong><small>关闭后减少轨道位移与页面过渡动画。</small></div>
            <el-switch v-model="motion" active-text="开" inactive-text="减" />
          </div>
          <div class="setting-row">
            <div><strong>紧凑信息密度</strong><small>在列表和后台表格中显示更多内容。</small></div>
            <el-switch v-model="compact" />
          </div>
        </section>
        <section id="editing">
          <header><span>编辑习惯</span><p>控制简历工坊保存内容的方式。</p></header>
          <div class="setting-row">
            <div><strong>自动保存</strong><small>开启后，编辑停止约 1.2 秒会自动保存当前版本。</small></div>
            <el-switch v-model="autosave" />
          </div>
          <div class="setting-row">
            <div><strong>默认纸张</strong><small>当前仅支持 A4 画布与导出尺寸。</small></div>
            <el-tag type="info" effect="plain">A4</el-tag>
          </div>
        </section>
        <section id="notice">
          <header><span>通知</span><p>偏好会保存在本设备，用于后续通知能力扩展。</p></header>
          <div class="setting-row">
            <div><strong>账号与导出提醒</strong><small>安全事件、导出完成和额度变更。</small></div>
            <el-switch v-model="emailNotice" />
          </div>
          <div class="setting-row">
            <div><strong>面试练习提醒</strong><small>按计划提示尚未完成的练习。</small></div>
            <el-switch v-model="interviewNotice" />
          </div>
        </section>
        <section id="privacy">
          <header><span>隐私与数据</span><p>云端数据导出与账号删除能力即将开放。</p></header>
          <div class="setting-row">
            <div><strong>导出个人数据</strong><small>下载账号资料和简历内容的副本。</small></div>
            <el-button disabled title="即将开放">即将开放</el-button>
          </div>
          <div class="setting-row danger">
            <div><strong>删除云端内容</strong><small>此操作需要再次验证身份，当前版本暂不提供自助删除。</small></div>
            <el-button type="danger" plain disabled>即将开放</el-button>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-layout { display: grid; grid-template-columns: 180px minmax(0,760px); gap: 54px; }
.settings-layout > nav { position: sticky; top: 92px; align-self: start; display: grid; }
.settings-layout > nav a { padding: 10px 0; border-bottom: 1px solid var(--line); color: var(--ink-2); text-decoration: none; }
.settings-layout > nav a:hover { color: var(--accent); }
.settings-sections { display: grid; gap: 42px; }
.settings-sections section { scroll-margin-top: 90px; }
.settings-sections section > header { padding-bottom: 13px; border-bottom: 1px solid var(--line); }
.settings-sections section > header span { font-size: 17px; font-weight: 800; }
.settings-sections section > header p { margin: 4px 0 0; color: var(--muted); }
.setting-row { min-height: 76px; display: flex; align-items: center; justify-content: space-between; gap: 24px; border-bottom: 1px solid var(--line); }
.setting-row > div:first-child { display: flex; flex-direction: column; }
.setting-row small { color: var(--muted); }
.setting-row :deep(.theme-trigger) { min-width: 140px; }
.setting-row :deep(.theme-menu) { left: auto; right: 0; bottom: auto; top: calc(100% + 8px); }
.danger strong { color: var(--danger); }
@media(max-width:700px){
  .settings-layout{grid-template-columns:1fr;gap:24px}
  .settings-layout>nav{position:static;display:flex;overflow-x:auto;gap:18px}
  .settings-layout>nav a{white-space:nowrap}
  .setting-row{align-items:flex-start;padding:16px 0;flex-direction:column;gap:10px}
}
</style>
