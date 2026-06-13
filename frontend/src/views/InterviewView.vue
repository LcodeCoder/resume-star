<!--
  模拟面试页面 - 沉浸式对话版
  设计参考：豆包 / ChatGPT 网页版的对话流，左右气泡 + 面试官头像 + 输入区贴底
  功能：
    1) 总时长来自管理员配置（不再写死 10 分钟）
    2) 单题不限时间，可一次性长篇作答；总时长到了或手动结束才生成报告
    3) 面试官头像 = 西装人像（替代之前的机器人）
    4) 每题 AI 回评、综合分、能力分布雷达图、鼓励语
-->
<script setup>
import { computed, onMounted, onUnmounted, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'
import { listResumes, listDraftResumes } from '../api/resume'
import {
  getInterviewQuota,
  getInterviewCategories,
  generateInterviewQuestion,
  submitInterview
} from '../api/interview'
import { getInterviewConfig } from '../api/interview'
import * as echarts from 'echarts'

const router = useRouter()
const userStore = useUserStore()

const stage = ref('intro') // intro | running | loading | report
const interviewQuota = ref(0)
const hasQuota = computed(() => interviewQuota.value > 0)

const pickerVisible = ref(false)
const pickerLoading = ref(false)
const resumeList = ref([])
const selectedResumeId = ref(null)
const categories = ref([])
const selectedCategoryCode = ref('general')

const currentUserId = () => userStore.profile?.id || 1

const cfg = ref({
  totalMinutes: 15,
  maxQuestions: 8,
  opening: '',
  selfIntroPrompt: '',
  dailyLimit: 3
})
const remainSeconds = ref(900)
let timerId = null

const messages = ref([])
const currentAnswer = ref('')
const chatContainer = ref(null)
const inputBox = ref(null)
const submitting = ref(false)
const aiThinking = ref(false)

const report = ref(null)
const radarChart = ref(null)

const interviewerAvatar = 'https://api.dicebear.com/7.x/personas/svg?seed=interviewer-pro&backgroundColor=1e3a8a,1e40af,2563eb&backgroundType=solid'

const timerText = computed(() => {
  const total = Math.max(0, remainSeconds.value)
  const m = Math.floor(total / 60)
  const s = total % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const timerDanger = computed(() => remainSeconds.value < 60)
const timerWarning = computed(() => remainSeconds.value < 180 && remainSeconds.value >= 60)

const answeredCount = computed(() => messages.value.filter((m) => m.role === 'user').length)
const questionCount = computed(() => messages.value.filter((m) => m.role === 'interviewer' && !m.opening).length)

const loadQuota = async () => {
  try {
    interviewQuota.value = await getInterviewQuota(currentUserId())
  } catch (e) {
    interviewQuota.value = 3
  }
}

const loadConfig = async () => {
  try {
    cfg.value = await getInterviewConfig()
    remainSeconds.value = (cfg.value.totalMinutes || 15) * 60
  } catch (e) {
    remainSeconds.value = 900
  }
}

const loadCategories = async () => {
  try {
    categories.value = await getInterviewCategories()
    if (categories.value.length > 0 && !categories.value.find((c) => c.code === selectedCategoryCode.value)) {
      selectedCategoryCode.value = categories.value[0].code
    }
  } catch (e) {
    categories.value = []
  }
}

const openPicker = async () => {
  if (!hasQuota.value) {
    router.push('/member')
    return
  }
  pickerVisible.value = true
  pickerLoading.value = true
  try {
    const userId = currentUserId()
    const [published, drafts] = await Promise.all([
      listResumes({ userId }).catch(() => []),
      listDraftResumes({ userId }).catch(() => [])
    ])
    const merged = [...(published || []), ...(drafts || [])]
    const seen = new Set()
    resumeList.value = merged.filter((r) => !seen.has(r.id) && seen.add(r.id))
    selectedResumeId.value = resumeList.value[0]?.id ?? null
  } finally {
    pickerLoading.value = false
  }
}

const selectedResume = computed(() =>
  resumeList.value.find((r) => r.id === selectedResumeId.value) || null
)

const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

const stopTimer = () => {
  if (timerId) {
    clearInterval(timerId)
    timerId = null
  }
}

const startTimer = () => {
  stopTimer()
  timerId = setInterval(() => {
    remainSeconds.value -= 1
    if (remainSeconds.value <= 0) {
      ElMessage.warning('面试时间到，正在生成报告...')
      finishInterview(true)
    }
  }, 1000)
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

const fmtNow = () => {
  const d = new Date()
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const resumeSnapshot = computed(() => {
  if (!selectedResume.value) return ''
  try {
    return JSON.stringify(selectedResume.value).slice(0, 4000)
  } catch (e) {
    return ''
  }
})

const buildHistoryForApi = () => {
  const history = []
  for (let i = 0; i < messages.value.length; i++) {
    const msg = messages.value[i]
    if (msg.role !== 'interviewer' || msg.opening) continue
    const userMsg = messages.value[i + 1]
    if (userMsg && userMsg.role === 'user') {
      history.push({ question: msg.content, answer: userMsg.content })
    } else {
      history.push({ question: msg.content, answer: '' })
    }
  }
  return history
}

const askNextQuestion = async () => {
  if (questionCount.value >= (cfg.value.maxQuestions || 10)) {
    pushInterviewerMessage('我的题目已经问完啦，可以点击右下角「结束面试」生成评测报告。', true)
    return
  }
  aiThinking.value = true
  scrollToBottom()
  try {
    const data = await generateInterviewQuestion({
      userId: currentUserId(),
      resumeContent: resumeSnapshot.value,
      categoryCode: selectedCategoryCode.value,
      history: buildHistoryForApi()
    })
    aiThinking.value = false
    pushInterviewerMessage(data.question || '请聊聊你最近做过的一个项目。')
  } catch (e) {
    aiThinking.value = false
    pushInterviewerMessage('请继续聊聊你最近做过的一个项目，遇到的难点和你的解决思路。')
  }
}

const pushInterviewerMessage = (content, opening = false) => {
  messages.value.push({
    role: 'interviewer',
    content,
    time: fmtNow(),
    opening
  })
  scrollToBottom()
}

const pushUserMessage = (content) => {
  messages.value.push({
    role: 'user',
    content,
    time: fmtNow()
  })
  scrollToBottom()
}

const startInterview = async () => {
  if (!selectedResumeId.value) {
    ElMessage.warning('请先选择一份简历')
    return
  }
  pickerVisible.value = false
  messages.value = []
  remainSeconds.value = (cfg.value.totalMinutes || 15) * 60
  stage.value = 'running'
  currentAnswer.value = ''
  startTimer()
  // 开场白（非问题，不计入题数）
  pushInterviewerMessage(cfg.value.opening || '您好！欢迎参加本次模拟面试。我会根据您的简历提问，请尽量结合具体经历与数据作答。', true)
  // 第一题：让 AI 出
  await askNextQuestion()
  nextTick(() => inputBox.value?.focus?.())
}

const submitAnswer = async () => {
  if (!currentAnswer.value.trim()) {
    ElMessage.warning('请输入你的回答')
    return
  }
  if (submitting.value) return
  submitting.value = true
  const answer = currentAnswer.value.trim()
  currentAnswer.value = ''
  pushUserMessage(answer)
  await askNextQuestion()
  submitting.value = false
}

const finishInterview = async (auto = false) => {
  if (!auto && answeredCount.value === 0) {
    ElMessage.warning('至少回答一题再结束面试')
    return
  }
  if (!auto) {
    try {
      await ElMessageBox.confirm('确定结束面试并生成报告吗？', '结束确认', { type: 'warning' })
    } catch {
      return
    }
  }
  if (currentAnswer.value.trim()) {
    pushUserMessage(currentAnswer.value.trim())
    currentAnswer.value = ''
  }
  stopTimer()
  stage.value = 'loading'

  const qaList = []
  for (let i = 0; i < messages.value.length; i++) {
    const msg = messages.value[i]
    if (msg.role !== 'interviewer' || msg.opening) continue
    const userMsg = messages.value[i + 1]
    qaList.push({
      question: msg.content,
      answer: userMsg && userMsg.role === 'user' ? userMsg.content : ''
    })
  }
  try {
    const used = (cfg.value.totalMinutes || 15) * 60 - remainSeconds.value
    const result = await submitInterview({
      userId: currentUserId(),
      resumeId: selectedResumeId.value,
      resumeTitle: selectedResume.value?.title || '未命名简历',
      resumeContent: resumeSnapshot.value,
      categoryCode: selectedCategoryCode.value,
      durationSeconds: Math.max(60, used),
      qaList
    })
    report.value = result
    stage.value = 'report'
    await loadQuota()
    nextTick(renderRadarChart)
  } catch (e) {
    ElMessage.error('生成报告失败：' + (e.message || '未知错误'))
    stage.value = 'running'
    startTimer()
  }
}

const renderRadarChart = () => {
  if (!radarChart.value || !report.value) return
  const chartInstance = echarts.init(radarChart.value)
  const abilityDist = report.value.abilityDistribution || {}
  const indicator = Object.keys(abilityDist).map((key) => ({ name: key, max: 100 }))
  const values = Object.values(abilityDist)

  chartInstance.setOption({
    color: ['#2563eb'],
    radar: {
      indicator,
      radius: '68%',
      splitNumber: 4,
      axisName: { color: '#1f2937', fontSize: 13, fontWeight: 500 },
      splitArea: { areaStyle: { color: ['rgba(37,99,235,0.04)', 'rgba(37,99,235,0.08)'] } },
      splitLine: { lineStyle: { color: 'rgba(37,99,235,0.18)' } },
      axisLine: { lineStyle: { color: 'rgba(37,99,235,0.18)' } }
    },
    series: [
      {
        type: 'radar',
        symbol: 'circle',
        symbolSize: 6,
        data: [
          {
            value: values,
            name: '能力分布',
            areaStyle: { color: 'rgba(37,99,235,0.18)' },
            lineStyle: { color: '#2563eb', width: 2 },
            itemStyle: { color: '#2563eb' }
          }
        ]
      }
    ]
  })
  window.addEventListener('resize', () => chartInstance.resize())
}

const scoreTone = (score) => (score >= 80 ? 'good' : score >= 60 ? 'mid' : 'bad')

const downloadReport = () => {
  const r = report.value
  if (!r) return
  const lines = [
    `# 模拟面试报告（${r.resumeTitle}）`,
    '',
    `**综合得分：${r.totalScore} 分**　共回答 ${r.answeredCount} 题`,
    '',
    `> ${r.summary}`,
    '',
    `> ${r.encouragement}`,
    ''
  ]
  if (r.qaDetail && r.qaDetail.length > 0) {
    r.qaDetail.forEach((item, i) => {
      lines.push(`## Q${i + 1}. ${item.question}`, '')
      lines.push(`**回答：** ${item.answer || '（未作答）'}`, '')
      lines.push(`- 得分：${item.score} 分`)
      lines.push(`- 薄弱点：${item.weak || '—'}`)
      lines.push(`- 提升建议：${item.advice || '—'}`, '')
    })
  }
  const url = URL.createObjectURL(new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `模拟面试报告-${new Date().toISOString().slice(0, 10)}.md`
  link.click()
  URL.revokeObjectURL(url)
}

const restart = () => {
  report.value = null
  stage.value = 'intro'
  loadQuota()
}

const viewHistory = () => {
  router.push('/profile?tab=interview')
}

onMounted(async () => {
  if (!userStore.profile) await userStore.loadProfile()
  await Promise.all([loadQuota(), loadConfig(), loadCategories()])
})

onUnmounted(stopTimer)
</script>

<template>
  <!-- 介绍页 -->
  <template v-if="stage === 'intro'">
    <section class="interview-hero">
      <div class="interview-hero-decor" aria-hidden="true"></div>
      <div class="interview-hero-inner">
        <div class="interview-badge">AI 模拟面试 · 沉浸式演练</div>
        <h2>{{ cfg.totalMinutes }} 分钟 AI 面试官，提前练手</h2>
        <p>
          选择简历与岗位方向，AI 面试官根据简历持续追问。回答不限时长，时间到自动生成包含
          逐题评分、能力雷达图、薄弱点剖析和鼓励语的报告。
        </p>
        <div class="interview-hero-actions">
          <el-button v-if="hasQuota" type="primary" size="large" @click="openPicker">开始模拟面试</el-button>
          <el-button v-else type="primary" size="large" @click="router.push('/member')">去获取面试次数</el-button>
          <el-button size="large" @click="viewHistory">查看历史记录</el-button>
        </div>
        <span class="interview-quota-text">今日剩余面试次数：{{ interviewQuota }} 次</span>
        <el-alert
          v-if="!hasQuota"
          class="interview-lock-alert"
          type="warning"
          :closable="false"
          show-icon
          title="面试次数已用完，开通会员或获取额度包可获得更多面试机会。"
        />
      </div>
    </section>

    <section class="interview-steps">
      <article class="card interview-step-card">
        <span class="interview-step-num">1</span>
        <h3>选择简历 + 岗位</h3>
        <p>从简历库选一份，并指定面试方向，AI 会按该方向出题</p>
      </article>
      <article class="card interview-step-card">
        <span class="interview-step-num">2</span>
        <h3>{{ cfg.totalMinutes }} 分钟对话</h3>
        <p>面试官持续追问，单题不限时长，自然组织语言作答</p>
      </article>
      <article class="card interview-step-card">
        <span class="interview-step-num">3</span>
        <h3>查看评测报告</h3>
        <p>逐题打分、能力雷达图、薄弱点、提升建议和鼓励语</p>
      </article>
    </section>
  </template>

  <!-- 答题页 - 对话式 -->
  <section v-else-if="stage === 'running'" class="chat-shell">
    <header class="chat-header">
      <div class="chat-header-left">
        <div class="chat-header-avatar">
          <img :src="interviewerAvatar" alt="面试官" />
        </div>
        <div class="chat-header-meta">
          <div class="chat-header-name">AI 面试官 · {{ categories.find((c) => c.code === selectedCategoryCode)?.name || '通用面试' }}</div>
          <div class="chat-header-sub">已提问 {{ questionCount }} / {{ cfg.maxQuestions }}　已回答 {{ answeredCount }} 题</div>
        </div>
      </div>
      <div class="chat-header-right">
        <div class="chat-timer" :class="{ danger: timerDanger, warning: timerWarning }">
          <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.6">
            <circle cx="8" cy="8" r="6.4" /><path d="M8 4.6V8l2.4 1.6" stroke-linecap="round" />
          </svg>
          {{ timerText }}
        </div>
        <el-button size="small" type="danger" plain @click="finishInterview(false)">结束面试</el-button>
      </div>
    </header>

    <div ref="chatContainer" class="chat-stream">
      <div v-for="(msg, idx) in messages" :key="idx" class="chat-row" :class="msg.role">
        <div class="chat-avatar">
          <img v-if="msg.role === 'interviewer'" :src="interviewerAvatar" alt="面试官" />
          <img v-else :src="userStore.profile?.avatar || 'https://api.dicebear.com/7.x/initials/svg?seed=User'" alt="我" />
        </div>
        <div class="chat-bubble-wrap">
          <div class="chat-name">{{ msg.role === 'interviewer' ? '面试官' : '我' }}<span class="chat-time">{{ msg.time }}</span></div>
          <div class="chat-bubble" :class="msg.role">
            <div class="chat-text">{{ msg.content }}</div>
          </div>
        </div>
      </div>
      <div v-if="aiThinking" class="chat-row interviewer">
        <div class="chat-avatar">
          <img :src="interviewerAvatar" alt="面试官" />
        </div>
        <div class="chat-bubble-wrap">
          <div class="chat-name">面试官<span class="chat-time">正在思考...</span></div>
          <div class="chat-bubble interviewer thinking">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          </div>
        </div>
      </div>
    </div>

    <footer class="chat-input">
      <div class="chat-input-box">
        <el-input
          ref="inputBox"
          v-model="currentAnswer"
          type="textarea"
          :rows="3"
          resize="none"
          :maxlength="2000"
          show-word-limit
          placeholder="请输入你的回答，Ctrl + Enter 提交"
          :disabled="submitting || aiThinking"
          @keydown.ctrl.enter="submitAnswer"
        />
        <div class="chat-input-actions">
          <span class="chat-input-tip">单题不限时长 · 总时长 {{ cfg.totalMinutes }} 分钟</span>
          <el-button
            type="primary"
            :loading="submitting || aiThinking"
            :disabled="!currentAnswer.trim()"
            @click="submitAnswer"
          >
            提交回答 (Ctrl + Enter)
          </el-button>
        </div>
      </div>
    </footer>
  </section>

  <!-- 加载中 -->
  <section v-else-if="stage === 'loading'" class="interview-loading card">
    <div class="loading-pulse"></div>
    <p>AI 面试官正在认真打分，请稍候 3-10 秒...</p>
  </section>

  <!-- 报告页 -->
  <template v-else-if="stage === 'report' && report">
    <section class="interview-report-hero card">
      <div class="interview-score-ring" :class="scoreTone(report.totalScore)">
        <strong>{{ report.totalScore }}</strong>
        <span>综合得分</span>
      </div>
      <div class="interview-report-meta">
        <h2>模拟面试报告</h2>
        <p class="interview-report-resume">
          {{ report.resumeTitle }} · {{ report.categoryName }} · 回答 {{ report.answeredCount }} 题 · 用时 {{ Math.floor(report.durationSeconds / 60) }} 分钟
        </p>
        <p class="interview-report-summary">{{ report.summary }}</p>
        <p class="interview-report-encouragement">{{ report.encouragement }}</p>
        <div class="interview-report-actions">
          <el-button type="primary" @click="downloadReport">下载报告</el-button>
          <el-button @click="restart">再来一次</el-button>
          <el-button @click="viewHistory">查看历史</el-button>
        </div>
      </div>
    </section>

    <section class="card interview-radar-section">
      <h3>能力分布</h3>
      <div ref="radarChart" style="width: 100%; height: 320px;"></div>
    </section>

    <section class="interview-report-list">
      <article v-for="(item, i) in report.qaDetail" :key="i" class="card interview-report-item">
        <header class="interview-report-item-head">
          <span class="interview-q-index">Q{{ i + 1 }}</span>
          <p class="interview-q-text">{{ item.question }}</p>
          <span class="interview-q-score" :class="scoreTone(item.score)">{{ item.score }} 分</span>
        </header>
        <div class="interview-answer-text">{{ item.answer || '（未作答）' }}</div>
        <div class="interview-score-bar">
          <i :class="scoreTone(item.score)" :style="{ width: item.score + '%' }" />
        </div>
        <dl class="interview-report-detail">
          <div>
            <dt>薄弱点</dt>
            <dd>{{ item.weak || '—' }}</dd>
          </div>
          <div>
            <dt>提升建议</dt>
            <dd>{{ item.advice || '—' }}</dd>
          </div>
          <div v-if="item.dimension">
            <dt>考察维度</dt>
            <dd>{{ item.dimension }}</dd>
          </div>
        </dl>
      </article>
    </section>
  </template>

  <!-- 简历选择弹窗 -->
  <el-dialog v-model="pickerVisible" title="开始一场面试" width="620px">
    <div v-loading="pickerLoading" class="interview-picker">
      <div class="picker-section">
        <div class="picker-section-title">选择面试方向</div>
        <div class="category-grid">
          <label
            v-for="item in categories"
            :key="item.code"
            class="category-chip"
            :class="{ active: selectedCategoryCode === item.code }"
          >
            <input v-model="selectedCategoryCode" type="radio" :value="item.code" name="interview-category" />
            <strong>{{ item.name }}</strong>
            <span>{{ item.description || '通用方向' }}</span>
          </label>
        </div>
      </div>

      <div class="picker-section">
        <div class="picker-section-title">选择一份简历</div>
        <el-empty v-if="!pickerLoading && !resumeList.length" description="简历库为空，先去创建一份简历吧">
          <el-button type="primary" @click="router.push('/editor')">去创建简历</el-button>
        </el-empty>
        <label
          v-for="item in resumeList"
          :key="item.id"
          class="interview-resume-option"
          :class="{ selected: selectedResumeId === item.id }"
        >
          <input v-model="selectedResumeId" type="radio" :value="item.id" name="interview-resume" />
          <div class="interview-resume-info">
            <span class="interview-resume-title">{{ item.title || '未命名简历' }}</span>
            <span class="interview-resume-time">更新于 {{ fmtTime(item.updateTime || item.createTime) }}</span>
          </div>
          <el-tag :type="item.draft ? 'info' : 'success'" size="small" effect="plain">
            {{ item.draft ? '草稿' : '正式' }}
          </el-tag>
        </label>
      </div>
    </div>
    <template #footer>
      <el-button @click="pickerVisible = false">取消</el-button>
      <el-button type="primary" :disabled="!selectedResumeId || !selectedCategoryCode" @click="startInterview">
        开始面试 · {{ cfg.totalMinutes }} 分钟
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
/* ============ 对话页布局 ============ */
.chat-shell {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 160px);
  min-height: 600px;
  background: linear-gradient(180deg, #f7faff 0%, #ffffff 50%);
  border-radius: 18px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.06);
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}
.chat-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.chat-header-avatar img {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: linear-gradient(135deg, #1e3a8a, #2563eb);
  object-fit: cover;
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.25);
}
.chat-header-name {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}
.chat-header-sub {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}
.chat-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.chat-timer {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  font-size: 14px;
  font-weight: 600;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.08);
  border-radius: 999px;
  font-variant-numeric: tabular-nums;
}
.chat-timer.warning {
  color: #d97706;
  background: rgba(217, 119, 6, 0.08);
}
.chat-timer.danger {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.1);
  animation: pulse 1.5s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.04); }
}

.chat-stream {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  scroll-behavior: smooth;
}
.chat-row {
  display: flex;
  gap: 12px;
  max-width: 760px;
  width: 100%;
}
.chat-row.interviewer { align-self: flex-start; }
.chat-row.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}
.chat-avatar img {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}
.chat-row.interviewer .chat-avatar img {
  background: linear-gradient(135deg, #1e3a8a, #2563eb);
}
.chat-bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.chat-name {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 8px;
}
.chat-row.user .chat-name {
  justify-content: flex-end;
}
.chat-time { color: #94a3b8; }
.chat-bubble {
  border-radius: 16px;
  padding: 12px 16px;
  font-size: 15px;
  line-height: 1.65;
  word-break: break-word;
  white-space: pre-wrap;
  box-shadow: 0 6px 24px rgba(15, 23, 42, 0.04);
  max-width: 100%;
}
.chat-bubble.interviewer {
  background: #ffffff;
  color: #0f172a;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-top-left-radius: 6px;
}
.chat-bubble.user {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  border-top-right-radius: 6px;
}
.chat-bubble.thinking {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 14px 18px;
}
.chat-bubble.thinking .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94a3b8;
  animation: bounce 1.2s ease-in-out infinite;
}
.chat-bubble.thinking .dot:nth-child(2) { animation-delay: 0.2s; }
.chat-bubble.thinking .dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 100% { transform: translateY(0); opacity: 0.6; }
  50% { transform: translateY(-4px); opacity: 1; }
}

.chat-input {
  padding: 16px 24px 24px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: saturate(180%) blur(12px);
  border-top: 1px solid rgba(15, 23, 42, 0.06);
}
.chat-input-box {
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 16px;
  padding: 12px;
  background: #fff;
  transition: 0.2s;
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.04);
}
.chat-input-box:focus-within {
  border-color: #2563eb;
  box-shadow: 0 4px 22px rgba(37, 99, 235, 0.18);
}
.chat-input-box :deep(.el-textarea__inner) {
  border: 0;
  box-shadow: none !important;
  padding: 6px 8px;
  font-size: 15px;
}
.chat-input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding: 0 4px;
}
.chat-input-tip {
  font-size: 12px;
  color: #64748b;
}

/* ============ loading ============ */
.interview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 24px;
  background: linear-gradient(180deg, #f7faff, #fff);
}
.loading-pulse {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: conic-gradient(from 0deg, transparent, #2563eb, transparent);
  animation: spin 1.2s linear infinite;
  position: relative;
}
.loading-pulse::after {
  content: '';
  position: absolute;
  inset: 6px;
  background: #fff;
  border-radius: 50%;
}
@keyframes spin { to { transform: rotate(360deg); } }
.interview-loading p {
  font-size: 16px;
  color: #475569;
}

/* ============ 报告 ============ */
.interview-radar-section {
  padding: 28px;
}
.interview-radar-section h3 {
  margin: 0 0 20px;
  font-size: 18px;
  font-weight: 600;
}
.interview-answer-text {
  margin: 12px 0;
  padding: 12px 14px;
  background: rgba(15, 23, 42, 0.04);
  border-left: 3px solid #2563eb;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.65;
  color: #334155;
  white-space: pre-wrap;
}
.interview-report-encouragement {
  font-size: 16px;
  color: #16a34a;
  font-weight: 500;
  margin-top: 10px;
}

/* ============ 选择弹窗 ============ */
.picker-section + .picker-section { margin-top: 20px; }
.picker-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 10px;
  letter-spacing: 0.6px;
}
.category-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.category-chip {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 12px;
  cursor: pointer;
  transition: 0.2s;
  background: #fff;
}
.category-chip input { position: absolute; opacity: 0; pointer-events: none; }
.category-chip strong { font-size: 14px; color: #0f172a; }
.category-chip span { font-size: 12px; color: #64748b; line-height: 1.5; }
.category-chip.active {
  border-color: #2563eb;
  background: rgba(37, 99, 235, 0.06);
  box-shadow: 0 6px 22px rgba(37, 99, 235, 0.15);
}

@media (max-width: 720px) {
  .chat-shell { height: calc(100vh - 120px); }
  .chat-stream { padding: 16px; }
  .chat-row { max-width: 100%; }
  .chat-input { padding: 12px; }
  .category-grid { grid-template-columns: 1fr; }
}
</style>
