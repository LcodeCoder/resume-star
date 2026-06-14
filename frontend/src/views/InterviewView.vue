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
import { useSpeech } from '../composables/useSpeech'
import LineIcon from '../components/common/LineIcon.vue'
import * as echarts from 'echarts'

const router = useRouter()
const userStore = useUserStore()

// 语音能力（沉浸式模式使用）：TTS 念问题 + STT 语音作答
const speech = useSpeech()

/**
 * 操作前登录校验：未登录弹提示并拦截。
 * 面试页允许匿名浏览介绍/配置，但「开始面试」「查看历史」等需账号的操作必须先登录。
 */
const requireLogin = () => {
  if (userStore.isLoggedIn) return true
  ElMessage.warning('请先登录后再操作')
  return false
}

const stage = ref('intro') // intro | running | loading | report
const mode = ref('text')   // text=标准文字面试 | immersive=沉浸式语音面试
const isImmersive = computed(() => mode.value === 'immersive')
const interviewQuota = ref(0)
const hasQuota = computed(() => interviewQuota.value > 0)
// 沉浸式单场消耗 cost 次，进入前需保证额度足够
const immersiveCost = computed(() => cfg.value.immersiveCost || 2)
const canStartImmersive = computed(() => interviewQuota.value >= immersiveCost.value)

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
  dailyLimit: 3,
  immersiveEnabled: true,
  immersiveCost: 2,
  immersiveMinutes: 30,
  ttsCloudEnabled: false
})
const remainSeconds = ref(900)
let timerId = null

// 当前模式的总时长（分钟）
const activeMinutes = computed(() => (isImmersive.value ? cfg.value.immersiveMinutes || 30 : cfg.value.totalMinutes || 15))

// ===== 云端语音合成（更自然的神经网络音色，后端代理）=====
const CLOUD_VOICES = [
  { id: 'zh-CN-XiaoxiaoNeural', name: '晓晓（女声·温柔）' },
  { id: 'zh-CN-XiaoyiNeural', name: '晓伊（女声·亲和）' },
  { id: 'zh-CN-YunxiNeural', name: '云希（男声·沉稳）' },
  { id: 'zh-CN-YunyangNeural', name: '云扬（男声·专业）' },
  { id: 'zh-CN-YunjianNeural', name: '云健（男声·浑厚）' },
  { id: 'zh-CN-YunxiaNeural', name: '云夏（男声·年轻）' }
]
// 引擎：cloud=云端音色 | browser=浏览器本地音色
const ttsEngine = ref('browser')
const cloudVoice = ref('zh-CN-XiaoxiaoNeural')
const cloudSpeaking = ref(false)
let cloudAudioEl = null
let cloudFallbackNotified = false

// 串行朗读队列：保证任意时刻只有一个声音，避免开场白与第一题并发导致“云端+浏览器”双声叠加。
// ttsGen 为代次：stopAllSpeaking 自增使所有在途/排队的朗读作废。
let ttsQueue = []
let ttsRunning = false
let ttsGen = 0

// 统一的“面试官正在说话”状态（云端 / 浏览器）
const ttsSpeaking = computed(() => cloudSpeaking.value || speech.speaking.value)

/** 云端合成并播放一段（被新朗读取代时静默放弃，不回退、不叠音） */
const playCloud = async (text, gen) => {
  try {
    // 后端只返回上游音频地址，由浏览器直连播放（绕开服务器到 CDN 的不稳链路）
    const api = `/api/interview/tts?text=${encodeURIComponent(text)}&voice=${encodeURIComponent(cloudVoice.value)}&speed=1.0`
    const res = await fetch(api, { credentials: 'include' })
    if (gen !== ttsGen) return            // 已被打断
    if (!res.ok) throw new Error('tts http ' + res.status)
    const data = await res.json()
    if (gen !== ttsGen) return
    const audioUrl = data && data.url
    if (!audioUrl) throw new Error('tts no url')
    if (!cloudAudioEl) cloudAudioEl = new Audio()
    // 跨域媒体播放无需 CORS；CDN 支持 Range，Safari 也能正常播放
    cloudAudioEl.src = audioUrl
    cloudSpeaking.value = true
    await new Promise((resolve) => {
      cloudAudioEl.onended = resolve
      cloudAudioEl.onerror = resolve   // 播放被打断/出错 → 静默结束
      const p = cloudAudioEl.play()
      if (p && p.catch) p.catch(() => resolve())
    })
    cloudSpeaking.value = false
  } catch (e) {
    cloudSpeaking.value = false
    if (gen !== ttsGen) return            // 被取代则不回退，避免叠音
    if (!cloudFallbackNotified) {
      ElMessage.info('云端音色暂不可用，已切换为浏览器语音')
      cloudFallbackNotified = true
    }
    await speech.speak(text)
  }
}

/** 朗读一段（按引擎选择云端/浏览器），完成后 resolve */
const speakOne = async (text, gen) => {
  if (ttsEngine.value === 'cloud' && cfg.value.ttsCloudEnabled) {
    await playCloud(text, gen)
  } else {
    await speech.speak(text)
  }
}

/** 驱动队列：逐条朗读，期间代次不变则继续 */
const runTtsQueue = async () => {
  if (ttsRunning) return
  ttsRunning = true
  const gen = ttsGen
  while (ttsQueue.length && gen === ttsGen) {
    const text = ttsQueue.shift()
    await speakOne(text, gen)
  }
  ttsRunning = false
}

/** 入队朗读（面试官每条消息用它，按顺序依次念，不叠音） */
const speak = (text) => {
  if (!text) return
  ttsQueue.push(text)
  runTtsQueue()
}

/** 立即朗读（试听/重听/切音色用）：先打断当前与队列，再念这一条 */
const speakNow = (text) => {
  stopAllSpeaking()
  speak(text)
}

/** 停止所有朗读：作废代次、清空队列、掐断云端与浏览器 */
const stopAllSpeaking = () => {
  ttsGen++
  ttsQueue = []
  ttsRunning = false
  speech.stopSpeaking()
  if (cloudAudioEl) {
    cloudAudioEl.onended = null
    cloudAudioEl.onerror = null
    try { cloudAudioEl.pause() } catch (e) { /* ignore */ }
  }
  cloudSpeaking.value = false
}

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
  // 未登录跳过额度查询（该接口需登录），避免触发 401 跳转
  if (!userStore.isLoggedIn) {
    interviewQuota.value = 0
    return
  }
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
    // 云端可用时默认用云端音色（更自然）；否则用浏览器
    ttsEngine.value = cfg.value.ttsCloudEnabled ? 'cloud' : 'browser'
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

const openPicker = async (pickMode = 'text') => {
  if (!requireLogin()) return
  mode.value = pickMode === 'immersive' ? 'immersive' : 'text'
  // 额度校验：沉浸式需 cost 次，标准需 1 次
  if (isImmersive.value ? !canStartImmersive.value : !hasQuota.value) {
    router.push('/member')
    return
  }
  if (isImmersive.value && !speech.sttSupported) {
    ElMessage.warning('当前浏览器不支持语音识别，建议用 Chrome / Edge 体验沉浸式语音面试')
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
      history: buildHistoryForApi(),
      immersive: isImmersive.value
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
  // 沉浸式：面试官的话用 TTS 念出来（云端或浏览器）
  if (isImmersive.value && content) {
    speak(content)
  }
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
  if (!requireLogin()) return
  if (!selectedResumeId.value) {
    ElMessage.warning('请先选择一份简历')
    return
  }
  pickerVisible.value = false
  messages.value = []
  remainSeconds.value = activeMinutes.value * 60
  stage.value = 'running'
  currentAnswer.value = ''
  startTimer()
  // 开场白（非问题，不计入题数）；沉浸式下会自动朗读
  pushInterviewerMessage(cfg.value.opening || '您好！欢迎参加本次模拟面试。我会根据您的简历提问，请尽量结合具体经历与数据作答。', true)
  // 第一题：让 AI 出
  await askNextQuestion()
  if (!isImmersive.value) nextTick(() => inputBox.value?.focus?.())
}

const submitAnswer = async () => {
  // 沉浸式下若正在录音，先停止并合并识别结果
  if (speech.listening.value) {
    const text = speech.stopListening()
    if (text) currentAnswer.value = text
  }
  if (!currentAnswer.value.trim()) {
    ElMessage.warning(isImmersive.value ? '请先点麦克风说出你的回答' : '请输入你的回答')
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

/** 沉浸式：切换麦克风录音；停止时把识别文字填进作答框 */
const toggleMic = () => {
  if (!speech.sttSupported) {
    ElMessage.warning('当前浏览器不支持语音识别，请改用 Chrome / Edge')
    return
  }
  if (speech.listening.value) {
    const text = speech.stopListening()
    if (text) currentAnswer.value = text
  } else {
    // 开始录音前掐断面试官朗读，避免麦克风录进 TTS 声音
    stopAllSpeaking()
    speech.startListening({ onfinal: (t) => { currentAnswer.value = t } })
  }
}

/** 沉浸式：重新朗读面试官最近一句话 */
const replayQuestion = () => {
  const last = [...messages.value].reverse().find((m) => m.role === 'interviewer')
  if (last) speakNow(last.content)
}

const VOICE_SAMPLE = '你好，我是你的 AI 面试官，我们现在开始吧。'

/** 切换引擎（云端 / 浏览器），并试听 */
const onEngineChange = (val) => {
  cloudFallbackNotified = false
  ttsEngine.value = val
  speakNow(VOICE_SAMPLE)
}

/** 切换浏览器音色并试听 */
const onBrowserVoiceChange = (uri) => {
  speech.setVoice(uri)
  speakNow(VOICE_SAMPLE)
}

/** 切换云端音色并试听 */
const onCloudVoiceChange = (id) => {
  cloudVoice.value = id
  speakNow(VOICE_SAMPLE)
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
  stopAllSpeaking()
  if (speech.listening.value) speech.stopListening()
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
    const used = activeMinutes.value * 60 - remainSeconds.value
    const result = await submitInterview({
      userId: currentUserId(),
      resumeId: selectedResumeId.value,
      resumeTitle: selectedResume.value?.title || '未命名简历',
      resumeContent: resumeSnapshot.value,
      categoryCode: selectedCategoryCode.value,
      durationSeconds: Math.max(60, used),
      qaList,
      immersive: isImmersive.value
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
  mode.value = 'text'
  stopAllSpeaking()
  stage.value = 'intro'
  loadQuota()
}

const viewHistory = () => {
  if (!requireLogin()) return
  router.push('/profile?tab=interview')
}

onMounted(async () => {
  if (!userStore.profile) await userStore.loadProfile()
  await Promise.all([loadQuota(), loadConfig(), loadCategories()])
})

onUnmounted(() => {
  stopTimer()
  stopAllSpeaking()
  speech.dispose()
})
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
          <el-button v-if="hasQuota || !userStore.isLoggedIn" type="primary" size="large" @click="openPicker('text')">开始标准面试</el-button>
          <el-button v-else type="primary" size="large" @click="router.push('/member')">去获取面试次数</el-button>
          <el-button
            v-if="cfg.immersiveEnabled"
            class="immersive-cta"
            size="large"
            @click="openPicker('immersive')"
          >
            <LineIcon name="mic" class="btn-lead-icon" /> 沉浸式语音面试
          </el-button>
          <el-button size="large" @click="viewHistory">查看历史记录</el-button>
        </div>
        <span v-if="userStore.isLoggedIn" class="interview-quota-text">
          今日剩余面试次数：{{ interviewQuota }} 次<template v-if="cfg.immersiveEnabled"> · 沉浸式语音面试单场消耗 {{ immersiveCost }} 次</template>
        </span>
        <el-alert
          v-if="userStore.isLoggedIn && !hasQuota"
          class="interview-lock-alert"
          type="warning"
          :closable="false"
          show-icon
          title="面试次数已用完，开通会员或获取额度包可获得更多面试机会。"
        />
      </div>
    </section>

    <!-- 沉浸式语音面试介绍卡 -->
    <section v-if="cfg.immersiveEnabled" class="card immersive-banner">
      <div class="immersive-banner-icon"><LineIcon name="headphones" /></div>
      <div class="immersive-banner-body">
        <h3>沉浸式语音面试</h3>
        <p>
          AI 面试官<strong>开口提问</strong>，你<strong>开口作答</strong>——全程语音对话，{{ cfg.immersiveMinutes }} 分钟拟真演练，
          面试官会照顾你的情绪、自然过渡追问。单场消耗 {{ immersiveCost }} 次面试额度。
        </p>
        <p class="immersive-banner-tip">建议使用 Chrome / Edge 浏览器并允许麦克风权限以获得最佳体验。</p>
        <div v-if="speech.ttsSupported || cfg.ttsCloudEnabled" class="voice-picker">
          <span class="voice-picker-label">面试官声音</span>
          <el-radio-group
            v-if="cfg.ttsCloudEnabled"
            :model-value="ttsEngine"
            size="small"
            @change="onEngineChange"
          >
            <el-radio-button value="cloud">云端音色（更自然）</el-radio-button>
            <el-radio-button value="browser">浏览器音色</el-radio-button>
          </el-radio-group>
          <!-- 云端音色下拉 -->
          <el-select
            v-if="ttsEngine === 'cloud' && cfg.ttsCloudEnabled"
            :model-value="cloudVoice"
            size="small"
            style="width: 200px"
            @change="onCloudVoiceChange"
          >
            <el-option v-for="v in CLOUD_VOICES" :key="v.id" :label="v.name" :value="v.id" />
          </el-select>
          <!-- 浏览器音色下拉 -->
          <el-select
            v-else-if="speech.zhVoices.value.length"
            :model-value="speech.selectedVoiceURI.value"
            size="small"
            style="width: 200px"
            placeholder="选择音色"
            @change="onBrowserVoiceChange"
          >
            <el-option v-for="v in speech.zhVoices.value" :key="v.voiceURI" :label="v.name" :value="v.voiceURI" />
          </el-select>
          <el-button size="small" text @click="speakNow(VOICE_SAMPLE)"><LineIcon name="volume" :size="17" class="btn-ico" /> 试听</el-button>
          <span v-if="ttsEngine === 'browser'" class="voice-picker-hint">浏览器音色推荐选带「Google」的更自然</span>
        </div>
      </div>
      <el-button
        type="primary"
        :disabled="userStore.isLoggedIn && !canStartImmersive"
        @click="openPicker('immersive')"
      >
        进入沉浸式
      </el-button>
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
          <div class="chat-header-name">
            AI 面试官 · {{ categories.find((c) => c.code === selectedCategoryCode)?.name || '通用面试' }}
            <span v-if="isImmersive" class="chat-mode-badge"><LineIcon name="mic" /> 语音</span>
          </div>
          <div class="chat-header-sub">
            已提问 {{ questionCount }} / {{ cfg.maxQuestions }}　已回答 {{ answeredCount }} 题
            <span v-if="isImmersive && ttsSpeaking" class="chat-speaking">· 面试官正在说话…</span>
          </div>
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
      <!-- 沉浸式语音作答工具条 -->
      <div v-if="isImmersive" class="voice-bar">
        <button
          type="button"
          class="mic-btn"
          :class="{ listening: speech.listening.value }"
          :disabled="submitting || aiThinking || ttsSpeaking"
          @click="toggleMic"
        >
          <span class="mic-dot"></span>
          {{ speech.listening.value ? '正在聆听，点此结束' : '按住说话 · 点击开始' }}
        </button>
        <el-button text size="small" :disabled="ttsSpeaking" @click="replayQuestion"><LineIcon name="volume" :size="17" class="btn-ico" /> 重听问题</el-button>
        <!-- 云端音色切换 -->
        <el-select
          v-if="ttsEngine === 'cloud' && cfg.ttsCloudEnabled"
          :model-value="cloudVoice"
          size="small"
          style="width: 160px"
          @change="onCloudVoiceChange"
        >
          <el-option v-for="v in CLOUD_VOICES" :key="v.id" :label="v.name" :value="v.id" />
        </el-select>
        <!-- 浏览器音色切换 -->
        <el-select
          v-else-if="speech.zhVoices.value.length"
          :model-value="speech.selectedVoiceURI.value"
          size="small"
          style="width: 160px"
          @change="onBrowserVoiceChange"
        >
          <el-option v-for="v in speech.zhVoices.value" :key="v.voiceURI" :label="v.name" :value="v.voiceURI" />
        </el-select>
        <span v-if="speech.interimText.value" class="voice-interim">{{ speech.interimText.value }}</span>
        <span v-else-if="!speech.sttSupported" class="voice-unsupported">当前浏览器不支持语音识别，可直接在下方打字作答</span>
      </div>
      <div class="chat-input-box">
        <el-input
          ref="inputBox"
          v-model="currentAnswer"
          type="textarea"
          :rows="3"
          resize="none"
          :maxlength="2000"
          show-word-limit
          :placeholder="isImmersive ? '语音识别结果会显示在这里，可直接修改后提交' : '请输入你的回答，Ctrl + Enter 提交'"
          :disabled="submitting || aiThinking"
          @keydown.ctrl.enter="submitAnswer"
        />
        <div class="chat-input-actions">
          <span class="chat-input-tip">单题不限时长 · 总时长 {{ activeMinutes }} 分钟<template v-if="isImmersive"> · 语音模式</template></span>
          <el-button
            type="primary"
            :loading="submitting || aiThinking"
            :disabled="!currentAnswer.trim() && !speech.listening.value"
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
  <el-dialog v-model="pickerVisible" :title="isImmersive ? '开始沉浸式语音面试' : '开始一场面试'" width="620px">
    <div v-loading="pickerLoading" class="interview-picker">
      <el-alert
        v-if="isImmersive"
        class="picker-mode-tip"
        type="info"
        :closable="false"
        show-icon
        :title="`语音模式：面试官会念出问题，你用麦克风作答 · 时长 ${cfg.immersiveMinutes} 分钟 · 本场消耗 ${immersiveCost} 次额度`"
      />
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
        开始{{ isImmersive ? '语音' : '' }}面试 · {{ activeMinutes }} 分钟
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

/* ===== 暗色模式：对话页 / 报告 / 选择弹窗（slate 调色板 → 深蓝） ===== */
html.dark .chat-shell {
  background: linear-gradient(180deg, #0d1530 0%, #0a1022 60%);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5);
}

html.dark .chat-header,
html.dark .chat-input {
  background: rgba(13, 20, 40, 0.75);
}

html.dark .chat-header {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

html.dark .chat-input {
  border-top-color: rgba(255, 255, 255, 0.08);
}

html.dark .chat-header-name { color: var(--color-text); }
html.dark .chat-header-sub,
html.dark .chat-name,
html.dark .chat-input-tip,
html.dark .picker-section-title { color: var(--color-text-secondary); }
html.dark .chat-time { color: var(--color-text-muted); }

html.dark .chat-bubble.interviewer {
  background: var(--color-surface);
  color: var(--color-text);
  border-color: rgba(255, 255, 255, 0.08);
}

html.dark .chat-input-box {
  background: var(--color-elevated);
  border-color: rgba(255, 255, 255, 0.12);
}

html.dark .chat-input-box :deep(.el-textarea__inner) {
  background: transparent;
  color: var(--color-text);
}

html.dark .interview-loading {
  background: linear-gradient(180deg, #0d1530, #0a1022);
}

html.dark .loading-pulse::after {
  background: var(--color-surface);
}

html.dark .interview-loading p { color: var(--color-text-secondary); }

html.dark .interview-answer-text {
  background: rgba(255, 255, 255, 0.05);
  color: var(--color-text-tertiary);
}

html.dark .category-chip {
  background: var(--color-elevated);
  border-color: rgba(255, 255, 255, 0.1);
}

html.dark .category-chip strong { color: var(--color-text); }
html.dark .category-chip span { color: var(--color-text-secondary); }

html.dark .category-chip.active {
  border-color: #4f86ff;
  background: rgba(79, 134, 255, 0.14);
  box-shadow: 0 6px 22px rgba(37, 99, 235, 0.25);
}

/* ===== 沉浸式语音面试 ===== */
.immersive-cta {
  background: linear-gradient(135deg, #7c3aed, #4f46e5);
  border: none;
  color: #fff;
  font-weight: 600;
}
.immersive-cta:hover { opacity: 0.92; color: #fff; }

.immersive-banner {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 18px;
  padding: 20px 24px;
}
.immersive-banner-icon { font-size: 34px; line-height: 1; color: #7c3aed; }
.immersive-banner-body { flex: 1; }
.immersive-banner-body h3 {
  margin: 0 0 6px;
  font-size: 17px;
  color: var(--color-text, #1d1d1f);
}
.immersive-banner-body p {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--color-text-muted, #6e6e73);
}
.immersive-banner-tip { margin-top: 4px !important; font-size: 12px !important; }

.voice-picker {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 12px;
}
.voice-picker-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-tertiary, #424245);
}
.voice-picker-hint { font-size: 12px; color: var(--color-text-muted, #6e6e73); }
.btn-ico { margin-right: 4px; }

.chat-mode-badge {
  margin-left: 8px;
  padding: 1px 8px;
  font-size: 12px;
  border-radius: var(--radius-pill, 999px);
  background: rgba(124, 58, 237, 0.12);
  color: #7c3aed;
}
.chat-speaking { color: #7c3aed; margin-left: 6px; }

.voice-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 10px 4px 4px;
}
.mic-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  border: 1px solid var(--color-border, #e8e8ed);
  border-radius: var(--radius-pill, 999px);
  background: var(--color-surface, #fff);
  color: var(--color-text-tertiary, #424245);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.mic-btn:hover:not(:disabled) { border-color: #7c3aed; color: #7c3aed; }
.mic-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.mic-btn .mic-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #c0c0c8;
}
.mic-btn.listening {
  border-color: #ef4444;
  color: #ef4444;
  background: rgba(239, 68, 68, 0.06);
}
.mic-btn.listening .mic-dot {
  background: #ef4444;
  animation: micPulse 1s ease-in-out infinite;
}
@keyframes micPulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.5); opacity: 0.5; }
}
.voice-interim {
  font-size: 13px;
  color: var(--color-text-muted, #6e6e73);
  font-style: italic;
}
.voice-unsupported { font-size: 12px; color: #e6a23c; }

@media (prefers-reduced-motion: reduce) {
  .mic-btn.listening .mic-dot { animation: none; }
}

html.dark .immersive-banner-body h3 { color: var(--color-text); }
html.dark .mic-btn { background: var(--color-elevated); border-color: rgba(255, 255, 255, 0.1); }
html.dark .chat-mode-badge { background: rgba(124, 58, 237, 0.22); color: #c4b5fd; }
</style>
