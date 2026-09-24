<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { listResumes } from '../api/resume'
import { askCareerCoach, loadCareerWorkspace, saveCareerWorkspace, appendCareerPractice, analyzeCareerEvidence } from '../api/careerLab'
import { evidenceGraph, flattenResume, exactExcerpt, sourcedResumeDraft } from '../utils/careerEvidence'
import { replayRubric } from '../utils/replayRubric'
import { extractTextFromFile } from '../utils/resumeImport'
import { useSpeech } from '../composables/useSpeech'
import { interviewerScenario, createInterviewerSession, candidateLabel, validateInterviewerDecision, interviewerReview } from '../utils/interviewerChallenge'

const user = useUserStore()
const route = useRoute()
const speech = useSpeech()
const tabs = [
  ['graph', '经历证据图谱', '岗位要求 → 经历 → 简历 → 回答'],
  ['campus', '校园经历考古机', '从课程、社团与竞赛挖掘真实贡献'],
  ['challenge', '岗位微实战副本', '用一份小作品补充能力证据'],
  ['interviewer', '面试官换位挑战', '向两位候选人追问，用证据做决定'],
  ['group', 'AI 群面攻防舱', '练习观点、反驳和推动共识'],
  ['contribution', '项目贡献拆解器', '分清团队成果与个人行动'],
  ['path', '求职岔路模拟器', '比较方向与下一步行动'],
  ['reverse', '反向面试训练', '学会询问岗位与培养机制'],
  ['counterfactual', '反事实面试舱', '变更约束后追问方案是否成立'],
  ['replay', '能力进化实验室', '同题重练并排对比']
]
const tab = ref(tabs.some(item => item[0] === route.query.tab) ? route.query.tab : 'overview')
const tabGroups = [
  { title: '从这里开始', items: tabs.filter(item => ['graph', 'campus', 'replay'].includes(item[0])) },
  { title: '面试与表达', items: tabs.filter(item => ['counterfactual', 'reverse', 'interviewer', 'group'].includes(item[0])) },
  { title: '探索与实战', items: tabs.filter(item => ['challenge', 'contribution', 'path'].includes(item[0])) }
]
const chooseTab = value => { tab.value = value; aiOutput.value = '' }
const replayOrigin = ref(null)
const counterMode = ref('技术面')
const blank = () => ({ jd: '', resumeId: null, experiences: [], practices: [], links: {}, notes: {}, pathA: '', pathB: '', interviewer: null })
const state = ref(blank())
const resumes = ref([])
const loaded = ref(false)
const saving = ref(false)
const working = ref(false)
const aiOutput = ref('')
const aiSource = ref('')
const form = ref({ title: '', description: '', source: '', evidence: '', question: '', answer: '', first: '', second: '', role: '', material: '', opponent: '', draftExperienceId: '', draftQuote: '' })
const selectedResume = computed(() => resumes.value.find(r => String(r.id) === String(state.value.resumeId)))
const graph = computed(() => evidenceGraph(state.value.jd, selectedResume.value, state.value.experiences, state.value.practices, state.value.links))
const evidenceAnalysis = ref(null)
const evidenceBusy = ref(false)
const evidenceError = ref('')
const evidenceDirty = ref(false)
const activeEvidence = computed(() => evidenceAnalysis.value?.requirements?.find(item => item.requirement === selectedNode.value?.requirement))
const linkDetails = ref(null)
const resumeParts = computed(() => flattenResume(selectedResume.value))
const selectedRequirement = ref('')
const selectedNode = computed(() => graph.value.find(n => n.requirement === selectedRequirement.value) || graph.value[0])
const emptyLink = () => ({ experienceId: '', experienceQuote: '', componentId: '', resumeQuote: '', answerId: '', answerQuote: '' })
const linkForm = ref(emptyLink())
watch(() => selectedNode.value?.requirement, requirement => { linkForm.value = { ...emptyLink(), ...(state.value.links?.[requirement] || {}) } }, { immediate: true })
const draft = ref(null)
const firstScore = computed(() => replayRubric(form.value.first))
const secondScore = computed(() => replayRubric(form.value.second))
const activePractice = computed(() => state.value.practices.filter(p => p.mode === tab.value).slice().reverse())
const spoken = ref(false)
const importing = ref(false)
const importMaterial = async event => {
  const file = event.target.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) { ElMessage.warning('材料请小于 5 MB'); return }
  importing.value = true
  try {
    const { text } = await extractTextFromFile(file)
    if (!text.trim()) throw new Error('未提取到文字；扫描 PDF 请先 OCR')
    form.value.source = `${file.name}：${text.slice(0, 2500)}`
    ElMessage.success('已提取文字，请核对个人贡献和材料来源')
  } catch (e) { ElMessage.error(e.message || '文件读取失败') }
  finally { importing.value = false; event.target.value = '' }
}
const importContribution = async event => { await importMaterial(event); form.value.material = form.value.source }
let saveTimer = null
let saveChain = Promise.resolve()

const persist = async () => {
  if (!loaded.value || !user.isLoggedIn) return
  const snapshot = JSON.parse(JSON.stringify(state.value))
  saving.value = true
  saveChain = saveChain.catch(() => {}).then(() => saveCareerWorkspace(snapshot))
  try { await saveChain; return true }
  catch (_) { ElMessage.error('工作区保存失败，请检查网络'); return false }
  finally { saving.value = false }
}
const scheduleSave = () => {
  if (!loaded.value) return
  clearTimeout(saveTimer)
  saveTimer = setTimeout(() => { saveTimer = null; persist() }, 700)
}
watch(state, scheduleSave, { deep: true })
// 分析基于已保存的经历；写入完成后才请求检索，避免新材料尚未入库。
let analysisSequence = 0
const refreshEvidence = async () => {
  if (!user.isLoggedIn || !state.value.jd?.trim()) return
  clearTimeout(saveTimer)
  saveTimer = null
  const sequence = ++analysisSequence
  evidenceBusy.value = true
  evidenceError.value = ''
  try {
    if (!(await persist())) throw new Error('工作区尚未保存，请检查网络后重试')
    const result = await analyzeCareerEvidence(state.value.jd)
    if (sequence !== analysisSequence) return
    evidenceAnalysis.value = result
    evidenceDirty.value = false
  } catch (error) {
    if (sequence !== analysisSequence) return
    evidenceAnalysis.value = null
    evidenceError.value = error.response?.status === 503 ? '语义检索暂不可用，请检查 Qdrant 和模型缓存' : (error.message || '语义检索暂不可用')
  } finally { if (sequence === analysisSequence) evidenceBusy.value = false }
}
const pickEvidence = async (requirement, hit) => {
  selectedRequirement.value = requirement
  await nextTick() // 等选中要求的表单同步完成，再填写新的推荐原句。
  // 自动填充只是待核对的草稿；最终仍走 saveLink 的逐字校验。
  linkForm.value = { ...emptyLink(), experienceId: hit.experienceId, experienceQuote: hit.quote }
  if (linkDetails.value) linkDetails.value.open = true
  ElMessage.info('已填入材料原句，请核对后点击“确认并保存关系”')
}
watch(() => [state.value.jd, state.value.experiences], () => {
  evidenceDirty.value = true
  ++analysisSequence // Edit invalidates an in-flight result; another analysis needs an explicit click.
  evidenceBusy.value = false
}, { deep: true })

onMounted(async () => {
  if (!user.isLoggedIn) { await user.loadProfile() }
  if (!user.isLoggedIn) { loaded.value = true; return }
  try {
    const [workspace, items] = await Promise.all([loadCareerWorkspace(), listResumes()])
    state.value = { ...blank(), ...(workspace || {}) }
    resumes.value = items || []
    if (route.query.resumeId && resumes.value.some(r => String(r.id) === String(route.query.resumeId))) state.value.resumeId = Number(route.query.resumeId)
    if (route.query.requirement) selectedRequirement.value = String(route.query.requirement)
    if (route.query.tab === 'replay') {
      try {
        const payload = JSON.parse(sessionStorage.getItem('career-lab-replay') || 'null')
        sessionStorage.removeItem('career-lab-replay')
        if (payload) {
          form.value.question = payload.question || ''
          form.value.first = payload.first || ''
          replayOrigin.value = payload
          selectedRequirement.value = payload.requirement || ''
        }
      } catch (_) { /* 忽略过期的会话数据 */ }
    }
  } catch (_) { ElMessage.warning('暂时无法读取职业实验室，请稍后重试') }
  loaded.value = true
})
onUnmounted(() => { if (saveTimer) { clearTimeout(saveTimer); persist() } speech.dispose() })
const addExperience = () => {
  if (!form.value.title.trim() || !form.value.description.trim()) return ElMessage.warning('请填写经历名称及本人行动')
  state.value.experiences.unshift({ id: crypto.randomUUID(), title: form.value.title.trim(), description: form.value.description.trim(), source: form.value.source.trim(), evidence: form.value.evidence.trim(), verified: !!form.value.evidence.trim(), createdAt: new Date().toISOString() })
  form.value.title = ''; form.value.description = ''; form.value.source = ''; form.value.evidence = ''
  ElMessage.success('经历已存入证据库；证据仍需人工核实')
}
const removeExperience = id => { state.value.experiences = state.value.experiences.filter(item => item.id !== id) }
const saveLink = () => {
  const requirement = selectedNode.value?.requirement
  if (!requirement) return
  const l = { ...linkForm.value }
  const exp = state.value.experiences.find(e => String(e.id) === String(l.experienceId))
  const part = resumeParts.value.find(c => String(c.id) === String(l.componentId))
  const answer = selectedNode.value.answers.find(a => String(a.id) === String(l.answerId))
  if (!exp || !exactExcerpt(`${exp.description || ''}\n${exp.source || ''}\n${exp.evidence || ''}`, l.experienceQuote)) return ElMessage.warning('先选择经历，并从经历行动、材料来源或核验记录中逐字填写引用')
  if (l.componentId && (!part || !exactExcerpt(part.text, l.resumeQuote))) return ElMessage.warning('简历引用必须逐字存在于所选简历段落')
  if (l.answerId && (!l.componentId || !answer || !exactExcerpt(answer.answer, l.answerQuote))) return ElMessage.warning('回答引用须逐字存在，且先连接简历段落')
  state.value.links = { ...state.value.links, [requirement]: l }
  ElMessage.success('证据关系已保存；请人工核对材料真实性')
}
const clearLink = () => {
  const next = { ...state.value.links }
  delete next[selectedNode.value?.requirement]
  state.value.links = next
  linkForm.value = emptyLink()
}
const makeDraft = () => {
  const exp = state.value.experiences.find(e => String(e.id) === String(form.value.draftExperienceId))
  draft.value = sourcedResumeDraft(exp, form.value.draftQuote)
  if (!draft.value) ElMessage.warning('引用必须逐字出现在该经历的材料来源中，至少 3 个字符')
}
const copyDraft = async () => {
  if (!draft.value) return
  try { await navigator.clipboard.writeText(draft.value.text); ElMessage.success('候选句已复制，请在简历工坊核实后采纳') }
  catch (_) { ElMessage.warning('复制失败，请手动选择候选句') }
}
const addPractice = (mode, extra = {}) => {
  const practice = { id: crypto.randomUUID(), mode, question: form.value.question.trim(), answer: form.value.answer.trim(), requirement: replayOrigin.value?.requirement || selectedNode.value?.requirement || '', createdAt: new Date().toISOString(), ...extra }
  state.value.practices.push(practice)
  appendCareerPractice(practice).catch(() => ElMessage.warning('本次练习记录同步失败，请检查网络'))
}
const assist = async (mode, context) => {
  if (!context?.trim()) return ElMessage.warning('先填写材料或作答')
  if (!user.isLoggedIn) return ElMessage.warning('登录后使用 AI 训练并保存进度')
  working.value = true
  try {
    const result = await askCareerCoach(mode, context)
    aiOutput.value = result.text || ''
    aiSource.value = result.source || 'local'
    if (aiSource.value === 'model') user.loadQuota()
    return aiOutput.value
  } catch (_) { return '' }
  finally { working.value = false }
}
const campusDig = async () => {
  const text = await assist('campus', `经历：${form.value.title}\n本人行动：${form.value.description}\n材料：${form.value.source}`)
  if (text) form.value.description = `${form.value.description}\n整理建议（请核实后采用）：${text}`.trim()
}
const runChallenge = async () => {
  const text = await assist('challenge', `目标岗位：${form.value.role}\nJD：${state.value.jd.slice(0, 1800)}\n已有证据：${state.value.experiences.map(e => e.title).join('、')}`)
  if (text) form.value.question = text
}
const reviewChallenge = async () => {
  const result = await assist('challenge-review', `题目：${form.value.question}\n作品：${form.value.answer}`)
  if (result) addPractice('challenge', { feedback: result })
}
const interviewerQuestion = ref('')
const interviewerSelected = ref('')
const interviewerReason = ref('')
const interviewerQuote = ref('')
const interviewer = computed(() => state.value.interviewer)
const interviewReview = computed(() => interviewer.value?.decision ? interviewerReview(interviewer.value) : null)
const startInterviewer = () => {
  if (!user.isLoggedIn) return ElMessage.warning('请先登录以保存演练进度')
  state.value.interviewer = createInterviewerSession()
  interviewerQuestion.value = '请介绍你亲自做过的报名流程改进，以及你如何判断它有效？'
  interviewerSelected.value = ''; interviewerReason.value = ''; interviewerQuote.value = ''
}
const askCandidates = async () => {
  const session = interviewer.value
  const question = interviewerQuestion.value.trim()
  if (!session || session.decision || working.value) return
  if (question.length < 6 || question.length > 220) return ElMessage.warning('请填写 6 至 220 字的问题')
  if (session.turns.length >= 3) return ElMessage.warning('本轮已提问三次，请完成判断后重新开局')
  working.value = true
  try {
    const answers = {}, sources = {}
    for (const kind of session.order) {
      const context = `岗位：${interviewerScenario.role}；任务：${interviewerScenario.task}\n候选人身份：${kind}\n事实卡：${interviewerScenario.cards[kind].facts.join('；')}\n第${session.turns.length + 1}轮提问：${question}\n此前回答：${session.turns.map(t => t.answers[kind]).join('；')}`
      const result = await askCareerCoach(`interviewer-${kind}`, context)
      if (!result?.text?.trim()) throw new Error('候选人回答为空')
      answers[kind] = result.text.trim().slice(0, 600)
      sources[kind] = result.source || 'local'
      if (sources[kind] === 'model') user.loadQuota()
    }
    session.turns.push({ question, answers, sources })
    interviewerQuestion.value = ''
    ElMessage.success(session.turns.length < 3 ? '已收到两位回答，请继续追问' : '三轮提问已完成，请做录用判断')
  } catch (_) { ElMessage.error('候选人回答失败，本轮没有保存；请重试') }
  finally { working.value = false }
}
const decideInterviewer = () => {
  const session = interviewer.value
  const error = validateInterviewerDecision(session, interviewerSelected.value, interviewerReason.value, interviewerQuote.value)
  if (error) return ElMessage.warning(error)
  session.decision = { selected: interviewerSelected.value, reason: interviewerReason.value.trim(), quote: interviewerQuote.value.trim(), decidedAt: new Date().toISOString() }
  addPractice('interviewer', { question: session.turns[0].question, role: session.role, order: [...session.order], turns: JSON.parse(JSON.stringify(session.turns)), decision: { ...session.decision }, feedback: interviewerReview(session).feedback })
  ElMessage.success('判断已保存，事实卡与复盘现已解锁')
}
const groupRoles = ['数据分析型同学', '执行落地型同学', '风险审查型同学']
const groupRoleIndex = ref(0)
const groupTurn = async () => {
  if (!form.value.question.trim() || !form.value.answer.trim()) return ElMessage.warning('请先填写议题和自己的观点')
  const role = groupRoles[groupRoleIndex.value % groupRoles.length]
  const reply = await assist('group', `你扮演${role}。话题：${form.value.question}\n上轮观点：${form.value.opponent}\n我的发言：${form.value.answer}`)
  if (reply) { addPractice('group', { feedback: `${role}：${reply}` }); form.value.opponent = `${role}：${reply}`; form.value.answer = ''; groupRoleIndex.value++; speak(reply) }
}
const compareReplay = async () => {
  if (!form.value.question.trim() || !form.value.first.trim() || !form.value.second.trim()) return ElMessage.warning('请填写题目和两次回答')
  const feedback = await assist('replay', `题目：${form.value.question}\n第一次：${form.value.first}\n第二次：${form.value.second}`)
  if (feedback) addPractice('replay', { first: form.value.first, second: form.value.second, feedback, previousScore: replayOrigin.value?.score, firstScore: firstScore.value, secondScore: secondScore.value })
}
const coach = async (mode, context) => {
  const feedback = await assist(mode, context)
  if (feedback) addPractice(mode, { feedback })
  return feedback
}
const speak = async text => {
  if (!speech.ttsSupported) return ElMessage.warning('浏览器暂不支持语音播报')
  spoken.value = true
  await speech.speak(text)
  spoken.value = false
}
const stopSpeech = () => { speech.stopSpeaking(); spoken.value = false }
const toggleDictation = target => {
  if (speech.listening.value) {
    const value = speech.stopListening()
    if (value) form.value[target] = (form.value[target] + ' ' + value).trim()
    return
  }
  if (!speech.sttSupported) return ElMessage.warning('浏览器不支持语音输入，请直接打字')
  speech.startListening({ onfinal: text => { form.value[target] = text }, onerror: () => ElMessage.warning('语音识别失败，请打字输入') })
}
const graphQuestion = () => {
  if (!selectedNode.value) return
  form.value.question = `关于“${selectedNode.value.requirement}”，请说明你亲自做了什么，依据是什么？`
  tab.value = 'counterfactual'
}
const counterTurns = computed(() => state.value.practices.filter(p => p.mode === 'counterfactual' && p.requirement === (selectedNode.value?.requirement || '')).slice(-4))
const askCounterfactual = async () => {
  const answer = form.value.answer.trim()
  if (!answer || !form.value.question.trim()) return ElMessage.warning('先回答当前问题再追问')
  addPractice('counterfactual', { scenario: counterMode.value })
  const context = `场景：${counterMode.value}\n岗位要求：${selectedNode.value?.requirement || ''}\n历史问答：${counterTurns.value.map(p => `问：${p.question}；答：${p.answer}`).join('\n')}`
  const question = await assist('counterfactual', context)
  if (question) { form.value.question = question; form.value.answer = '' }
}
const checkConsistency = () => coach('consistency', counterTurns.value.map(p => `${p.scenario || '场景'}：问题：${p.question}；回答：${p.answer}`).join('\n'))
const addGraphAnswer = () => {
  if (!form.value.answer.trim() || !form.value.question.trim()) return ElMessage.warning('先输入题目和回答')
  addPractice('counterfactual')
  form.value.answer = ''
  ElMessage.success('已关联到该岗位要求')
}
</script>

<template>
  <div class="lab">
    <header class="lab-hero"><span class="eyebrow">CAREER LAB · 校园职业实验室</span><h1>把经历变成证据，把练习变成进步。</h1><p>从岗位要求出发，连接校园项目、简历原文和面试回答。模型建议仅作练习辅助，作品事实与引用由你确认。</p><span v-if="saving" class="save-hint">保存中…</span><span v-else-if="user.isLoggedIn" class="save-hint">进度自动保存至账户</span></header>
    <div v-if="!user.isLoggedIn" class="lab-notice">登录后可使用 AI 辅助、保存经历证据和练习记录。 <router-link to="/login?redirect=/career-lab">前往登录 →</router-link></div>
    <div class="lab-layout">
      <nav class="lab-nav" aria-label="职业实验室功能">
        <button class="lab-home" :class="{ active: tab === 'overview' }" @click="chooseTab('overview')"><strong>使用指引</strong><small>三步开始，不用先配齐资料</small></button>
        <div v-for="group in tabGroups" :key="group.title" class="lab-nav-group"><span class="lab-nav-title">{{ group.title }}</span><button v-for="item in group.items" :key="item[0]" :class="{ active: tab === item[0] }" @click="chooseTab(item[0])"><strong>{{ item[1] }}</strong><small>{{ item[2] }}</small></button></div>
      </nav>
      <main class="lab-panel">
        <template v-if="tab === 'overview'">
          <h2>从一件真实经历开始</h2><p class="lead">材料可以分次补，右侧每个练习独立使用；内容自动保存，下次登录接着做。</p>
          <div class="lab-steps">
            <button @click="chooseTab('campus')"><span>01 · 准备材料</span><strong>写下我做过什么</strong><small>用课程、社团或项目经历起步，也可以导入文档。</small><em>{{ state.experiences.length }} 条经历已保存 →</em></button>
            <button @click="chooseTab('graph')"><span>02 · 连接岗位</span><strong>看看哪些要求有证据</strong><small>粘贴岗位 JD；简历可选，先看匹配线索再补原文。</small><em>{{ graph.length }} 条岗位要求 →</em></button>
            <button @click="chooseTab('replay')"><span>03 · 练习表达</span><strong>同一道题回答两遍</strong><small>对比行动、依据和结果，形成下一次的改进点。</small><em>进入练习 →</em></button>
          </div>
          <p class="lab-next">想直接练面试？左侧「面试与表达」中的模块无需先完成前面的步骤。</p>
        </template>
        <template v-else-if="tab === 'graph'"><h2>经历证据图谱</h2><p class="lead">逐条对照 JD。中文语义检索、BM25 和 RRF 给出材料线索；推荐仅供核对，逐字确认后才形成证据链。</p>
          <div class="field"><label>目标岗位 / JD</label><textarea v-model="state.jd" rows="6" placeholder="每行写一条要求，例如：熟悉 Vue 3；能独立完成接口设计；具备团队协作能力"></textarea></div>
          <div class="field"><label>选择个人简历（可选）</label><el-select v-model="state.resumeId" placeholder="可跳过：先填写岗位要求" clearable><el-option v-for="r in resumes" :key="r.id" :label="r.title" :value="r.id" /></el-select></div>
          <section class="search-summary" aria-live="polite">
            <div class="search-heading"><div><h3>岗位材料检索与项目推荐</h3><p class="muted">从下方经历库的真实原句中寻找线索。会员专属，修改 JD 或经历后点击分析；每日次数由后台会员套餐配置。</p></div><button class="inline" :disabled="evidenceBusy || !state.jd.trim() || !user.isLoggedIn || !user.vipLevel" @click="refreshEvidence()">{{ evidenceBusy ? '检索中…' : '开始分析' }}</button></div>
            <p v-if="!user.vipLevel" class="muted">该分析为会员权益；<router-link to="/member">查看会员方案 →</router-link>。原有的人工证据链仍可使用。</p>
            <p v-if="evidenceDirty && evidenceAnalysis" class="muted">材料已修改，点击“开始分析”更新结果。</p>
            <p v-if="evidenceBusy" class="muted">正在同步个人 Qdrant 索引并融合中文词项与语义检索，首次下载模型可能较久…</p>
            <p v-if="evidenceError" class="search-error">{{ evidenceError }}。下方仍可用原有关键词线索手工建立关系。</p>
            <template v-if="evidenceAnalysis && !evidenceDirty && !evidenceBusy">
              <p class="muted">{{ evidenceAnalysis.model }} · {{ evidenceAnalysis.ranking }} · 今日剩余 {{ evidenceAnalysis.quotaRemaining }} 次 · 已覆盖权重 {{ evidenceAnalysis.recommendation.coveredWeight }} / {{ evidenceAnalysis.recommendation.totalWeight }}（材料线索，尚未核实）</p>
              <div v-if="evidenceAnalysis.recommendation.projects.length" class="recommend-grid">
                <article v-for="(project, index) in evidenceAnalysis.recommendation.projects" :key="project.experienceId" class="recommend-card">
                  <small>推荐 {{ index + 1 }} · 新增覆盖权重 {{ project.gain }}</small><h4>{{ project.title }}</h4>
                  <div v-for="covered in project.covers" :key="covered.requirement" class="coverage-row"><b>{{ covered.requirement }}</b><blockquote>{{ covered.quote }}</blockquote><button class="inline" @click="pickEvidence(covered.requirement, { experienceId: project.experienceId, quote: covered.quote })">核对这句原文 →</button></div>
                </article>
              </div>
              <p v-else class="muted">当前经历未找到足够的岗位要求材料；可先在经历库添加真实项目。</p>
              <div v-if="evidenceAnalysis.recommendation.uncovered.length" class="coverage-gap"><b>尚未找到可支持的材料</b><p>{{ evidenceAnalysis.recommendation.uncovered.join('；') }}</p></div>
            </template>
          </section>
          <div class="graph-grid"><div class="node-list"><button v-for="node in graph" :key="node.id" :class="{ selected: selectedNode?.id === node.id }" @click="selectedRequirement = node.requirement"><span :class="node.missing ? 'gap' : 'found'">{{ node.chain.answer ? '完整证据链' : node.chain.experience ? '部分关系已确认' : node.evidence.length ? '仅有关键词线索' : '待补证据' }}</span>{{ node.requirement }}</button><p v-if="!graph.length" class="muted">先粘贴 JD，或从「校园经历考古机」保存一条经历。简历和回答可以以后再补。</p></div>
          <div v-if="selectedNode" class="node-detail"><h3>{{ selectedNode.requirement }}</h3><div class="chain-title">岗位要求 ↓ 经历与简历原文 ↓ 面试回答</div><div class="chain-flow"><span>{{ selectedNode.requirement }}</span><span>{{ selectedNode.chain.experience?.title || '经历证据待确认' }}</span><span>{{ selectedNode.chain.resume?.label || '简历表述待确认' }}</span><span>{{ selectedNode.chain.answer ? '回答原文已确认' : '面试回答待确认' }}</span></div><details ref="linkDetails" class="lab-advanced"><summary>进阶 · 核对并连接原文证据</summary><p class="muted">先在经历库保存事实，选择对应经历并摘录原句；简历和回答以后再补。</p><div class="field"><label>建立可核对关系 · 经历卡</label><el-select v-model="linkForm.experienceId" placeholder="选择经历" clearable><el-option v-for="e in state.experiences" :key="e.id" :label="e.title" :value="e.id" /></el-select><textarea v-model="linkForm.experienceQuote" rows="2" placeholder="从该经历行动或材料来源中复制原句"></textarea></div>
            <div class="field"><label>对应简历段落（可稍后补齐）</label><el-select v-model="linkForm.componentId" placeholder="可跳过：选择简历段落" clearable filterable><el-option v-for="c in resumeParts" :key="c.id" :label="`${c.label} · ${c.text.slice(0, 35)}`" :value="c.id" /></el-select><textarea v-model="linkForm.resumeQuote" rows="2" placeholder="从所选简历段落复制原句"></textarea></div>
            <div class="field"><label>对应面试回答（可稍后补齐）</label><el-select v-model="linkForm.answerId" placeholder="可跳过：选择面试回答" clearable filterable><el-option v-for="a in selectedNode.answers" :key="a.id" :label="`${a.question} · ${a.answer.slice(0, 30)}`" :value="a.id" /></el-select><textarea v-model="linkForm.answerQuote" rows="2" placeholder="从所选回答复制原句"></textarea></div>
            <button class="primary" @click="saveLink">确认并保存关系</button><button class="inline" @click="clearLink">清除关系</button></details>
            <article v-if="selectedNode.chain.experience" class="evidence"><b>已确认的经历原句</b><blockquote>{{ selectedNode.chain.experience.quote }}</blockquote><template v-if="selectedNode.chain.resume"><b>对应简历原句</b><blockquote>{{ selectedNode.chain.resume.quote }}</blockquote></template><template v-if="selectedNode.chain.answer"><b>对应回答原句</b><blockquote>{{ selectedNode.chain.answer.quote }}</blockquote></template></article>
            <template v-if="activeEvidence && !evidenceDirty && !evidenceBusy"><p class="muted">以下为 BM25 与 Qdrant 融合检索的候选原句；排名不等于真实性核验。</p>
              <article v-for="(hit, index) in activeEvidence.matches" :key="hit.experienceId + hit.field + index" class="evidence"><b>{{ hit.title }} · {{ hit.source }} <small v-if="hit.supported">候选线索</small></b><blockquote>{{ hit.quote }}</blockquote><small>词项排名：{{ hit.bm25Rank || '未命中' }} · 语义排名：{{ hit.vectorRank || '未命中' }} · RRF：{{ hit.rrfScore }}</small><div><button class="inline" @click="pickEvidence(selectedNode.requirement, hit)">带入逐字核对 →</button></div></article>
              <p v-if="!activeEvidence.matches.length" class="muted">当前要求暂无候选原句。</p>
            </template>
            <p v-else class="muted">以下仅为本地关键词线索，未确认前不形成图谱连线。</p><article v-if="!activeEvidence || evidenceDirty || evidenceBusy" v-for="item in selectedNode.evidence" :key="item.source + item.id" class="evidence"><b>{{ item.source }} · {{ item.label }}</b><blockquote>{{ item.text }}</blockquote><small>重合线索：{{ item.overlap.join('、') }} · 请人工确认关联性</small></article><p v-if="selectedNode.missing" class="muted">尚未确认经历证据。可在经历库添加真实材料，再逐字确认来源。</p><article v-for="(a, i) in selectedNode.answers" :key="i" class="evidence"><b>面试回答 · {{ a.question }}</b><blockquote>{{ a.answer }}</blockquote></article><button class="primary" @click="graphQuestion">围绕这一要求练习追问 →</button></div></div>
        </template>
        <template v-else-if="tab === 'campus'"><h2>校园经历考古机 · 可验证经历库</h2><p class="lead">没实习也可以从真实的课程、比赛、社团和志愿活动开始。只保存你亲自做过的事情；截图和链接由本人确认。</p>
          <div class="field"><label>经历名称</label><input v-model="form.title" placeholder="例：校园二手交易小程序课程设计" /></div><div class="field"><label>本人做了什么</label><textarea v-model="form.description" rows="5" placeholder="我负责的具体部分、遇到的困难、怎么解决；不知道的数字不要填"></textarea><button class="inline" :disabled="working" @click="campusDig">AI 追问与整理</button></div><div class="field"><label>材料来源</label><input v-model="form.source" placeholder="课程、比赛、社团；也可以粘贴材料摘要" /><label class="file-action">导入 TXT / DOCX / 可选中文字 PDF <input type="file" accept=".txt,.docx,.pdf" :disabled="importing" @change="importMaterial" /></label><small>文件只在浏览器提取文字；保存经历卡后，截取的材料文字会作为个人工作区数据保存。</small></div><div class="field"><label>可核实证据</label><input v-model="form.evidence" placeholder="仓库链接、作品链接、截图说明等（不会自动验证真伪）" /></div><button class="primary" @click="addExperience">保存经历卡</button><h3>从材料生成简历候选句</h3><p class="muted">选择经历并摘录材料原句。候选句只引用材料中已有文字；数字不会由系统补写，采纳前仍需核实本人贡献。</p><div class="field"><label>选择经历</label><el-select v-model="form.draftExperienceId" placeholder="请选择经历" clearable @change="draft = null"><el-option v-for="e in state.experiences" :key="e.id" :label="e.title" :value="e.id" /></el-select><label>材料原句</label><textarea v-model="form.draftQuote" rows="3" placeholder="粘贴上述经历材料中的连续原文" @input="draft = null"></textarea></div><button class="primary" @click="makeDraft">生成带引用候选句</button><div v-if="draft" class="evidence"><b>候选句 · 原文摘录</b><blockquote>{{ draft.text }}</blockquote><small>依据：{{ draft.quote }}</small><div><button class="inline" @click="copyDraft">复制并前往简历工坊人工采纳</button><router-link to="/editor">打开简历工坊 →</router-link></div></div><h3>我的经历证据</h3><div v-for="e in state.experiences" :key="e.id" class="record"><b>{{ e.title }}</b><span>{{ e.verified ? '已提供证据线索（待人工核实）' : '待补证据' }}</span><p>{{ e.description }}</p><small>来源：{{ e.source || '未填写' }} · 证据：{{ e.evidence || '未填写' }}</small><button class="inline" @click="removeExperience(e.id)">删除</button></div>
        </template>
        <template v-else-if="tab === 'challenge'"><h2>岗位微实战副本</h2><p class="lead">生成一道可完成的小任务，提交自己的作品，再把真实成果写回经历库。</p><div class="field"><label>目标岗位</label><input v-model="form.role" placeholder="如前端开发、产品经理、运营" /></div><button class="primary" :disabled="working" @click="runChallenge">生成 15 分钟小任务</button><div class="field"><label>任务与评价标准</label><textarea v-model="form.question" rows="6" placeholder="这里可以自己改写任务"></textarea></div><div class="field"><label>我的作品 / 解题过程</label><textarea v-model="form.answer" rows="7" placeholder="写下可核实的过程和交付物；可附上链接"></textarea></div><button class="primary" :disabled="working" @click="reviewChallenge">提交作品并复盘</button></template>
        <template v-else-if="tab === 'interviewer'">
          <h2>面试官换位挑战</h2><p class="lead">你担任面试官。向两位匿名候选人提出同一道题，再追问两轮，引用原句做出判断。所有候选人和资料均为教学模拟。</p>
          <article class="record"><b>{{ interviewerScenario.role }}</b><p>{{ interviewerScenario.task }}</p><small>两位候选人有相近的校园经历；事实卡将在选择后揭晓。</small></article>
          <button class="primary" :disabled="working" @click="startInterviewer">{{ interviewer ? '重新开局' : '开始挑战' }}</button>
          <template v-if="interviewer">
            <p class="chain-title">第 {{ Math.min(interviewer.turns.length + 1, 3) }} / 3 轮 · {{ interviewer.turns.length === 0 ? '首次提问' : '追问' }}</p>
            <div v-for="(turn, index) in interviewer.turns" :key="index" class="record">
              <b>第 {{ index + 1 }} 轮 · 我的问题：{{ turn.question }}</b>
              <div v-for="kind in interviewer.order" :key="kind" class="evidence"><strong>候选人 {{ candidateLabel(interviewer, kind) }}</strong><small> · {{ turn.sources[kind] === 'model' ? '模型模拟' : '本地演练' }}</small><blockquote>{{ turn.answers[kind] }}</blockquote></div>
            </div>
            <template v-if="!interviewer.decision && interviewer.turns.length < 3"><div class="field"><label for="interviewer-question">同一道题问两位候选人</label><textarea id="interviewer-question" v-model="interviewerQuestion" rows="3" maxlength="220" placeholder="例如：你本人完成了什么？能提供哪些材料核对？"></textarea></div><button class="primary" :disabled="working" @click="askCandidates">{{ working ? '两位候选人回答中…' : '同时提问两位候选人' }}</button></template>
            <template v-if="interviewer.turns.length >= 3 && !interviewer.decision">
              <h3>作出选择</h3><div class="field"><label for="interviewer-select">更愿意推进哪位候选人？</label><el-select id="interviewer-select" v-model="interviewerSelected" placeholder="选择候选人"><el-option v-for="kind in interviewer.order" :key="kind" :label="`候选人 ${candidateLabel(interviewer, kind)}`" :value="kind" /></el-select></div>
              <div class="field"><label for="interviewer-reason">判断理由（指出个人行动和证据）</label><textarea id="interviewer-reason" v-model="interviewerReason" rows="3" maxlength="500" placeholder="具体哪句话影响了你？哪些部分还要核实？"></textarea></div>
              <div class="field"><label for="interviewer-quote">从所选候选人的回答中逐字摘录一句</label><textarea id="interviewer-quote" v-model="interviewerQuote" rows="2" maxlength="300" placeholder="复制上方回答的一段原文"></textarea></div><button class="primary" @click="decideInterviewer">提交判断并查看事实卡</button>
            </template>
            <template v-if="interviewer.decision"><h3>复盘 · 候选人 {{ candidateLabel(interviewer, interviewer.decision.selected) }}</h3><article class="record"><b>你引用的原句</b><blockquote>{{ interviewer.decision.quote }}</blockquote><p>你的理由：{{ interviewer.decision.reason }}</p><p>{{ interviewReview.feedback }}</p><p>理由中{{ interviewReview.checkedContribution ? '提到了个人贡献' : '还可以追问个人分工' }}；{{ interviewReview.checkedEvidence ? '提到了核对材料' : '还可以索要可核对材料' }}。</p></article>
              <div class="comparison"><article v-for="kind in interviewer.order" :key="kind" class="record"><h3>候选人 {{ candidateLabel(interviewer, kind) }} 的模拟事实卡</h3><p>{{ interviewerScenario.cards[kind].profile }}</p><ul><li v-for="fact in interviewerScenario.cards[kind].facts" :key="fact">{{ fact }}</li></ul></article></div><p class="muted">事实卡是本练习的预设资料，模型回答如超出事实卡应继续核对；演练判断不等于真实招聘结论。</p>
            </template>
          </template>
        </template>
        <template v-else-if="tab === 'group'"><h2>AI 群面攻防舱</h2><p class="lead">三名轮流发言的 AI 同学分别关注数据、落地和风险；你负责提出理由、回应质疑、推动小组达成结论。</p><div class="field"><label>讨论议题</label><input v-model="form.question" placeholder="如：校园活动经费减半，应优先保留哪项？" /></div><div v-if="form.opponent" class="record"><b>同学的观点</b><p>{{ form.opponent }}</p><button class="inline" @click="speak(form.opponent)">朗读观点</button></div><div class="field"><label>我的发言</label><textarea v-model="form.answer" rows="5"></textarea><button class="inline" @click="toggleDictation('answer')">{{ speech.listening.value ? '停止语音输入' : '语音输入' }}</button></div><button class="primary" :disabled="working" @click="groupTurn">发言并听取反方观点</button><button class="inline" :disabled="working" @click="coach('group-review', state.practices.filter(p => p.mode === 'group').map(p => `我：${p.answer}；对方：${p.feedback}`).join('\n'))">复盘讨论</button></template>
        <template v-else-if="tab === 'contribution'"><h2>项目贡献拆解器</h2><p class="lead">团队作品中的个人贡献边界，值得比“负责整个项目”写得更清楚。</p><div class="field"><label>项目 / 团队材料摘要</label><textarea v-model="form.material" rows="5" placeholder="粘贴项目描述、分工记录或自己的工作日志"></textarea><label class="file-action">导入材料 <input type="file" accept=".txt,.docx,.pdf" :disabled="importing" @change="importContribution" /></label></div><div class="field"><label>我具体做了什么</label><textarea v-model="form.answer" rows="4"></textarea></div><button class="primary" :disabled="working" @click="coach('contribution', `团队材料：${form.material}\n本人陈述：${form.answer}`)">拆解个人贡献</button><p class="muted">材料由你输入；系统不自动验证 Git 历史或其他成员陈述。</p></template>
        <template v-else-if="tab === 'path'"><h2>求职岔路模拟器</h2><p class="lead">不猜录用率。用现有证据和一个最小试做任务来比较不同方向。</p><div class="field"><label>方向 A</label><input v-model="state.pathA" placeholder="例如前端开发" /></div><div class="field"><label>方向 B</label><input v-model="state.pathB" placeholder="例如产品经理" /></div><div class="field"><label>我的偏好与限制</label><textarea v-model="form.material" rows="4"></textarea></div><button class="primary" :disabled="working" @click="coach('path', `方向A：${state.pathA}；方向B：${state.pathB}；偏好：${form.material}；已有经历：${state.experiences.map(e => e.description).join('；')}`)">对比两条路线</button></template>
        <template v-else-if="tab === 'reverse'"><h2>反向面试训练</h2><p class="lead">练习向面试官问清真实工作内容、带教方式与考核标准。</p><div class="field"><label>岗位背景</label><input v-model="form.role" placeholder="目标岗位与关心的问题" /></div><div class="field"><label>我要向面试官提问</label><textarea v-model="form.answer" rows="4"></textarea></div><button class="primary" :disabled="working" @click="coach('reverse', `岗位：${form.role}\n提问：${form.answer}`)">让 AI 面试官回答</button></template>
        <template v-else-if="tab === 'counterfactual'"><h2>反事实面试舱 · 可追问的证据链</h2><p class="lead">回答后改变一个关键条件，观察方案是否仍成立；题目与回答会关联至当前岗位要求。</p><p v-if="selectedNode" class="chain-title">关联岗位要求：{{ selectedNode.requirement }}</p><div class="field"><label>当前题目</label><textarea v-model="form.question" rows="3" placeholder="例如：你如何解决项目中的接口性能问题？"></textarea><button class="inline" @click="speak(form.question)">朗读问题</button></div><div class="field"><label>我的回答</label><textarea v-model="form.answer" rows="5"></textarea><button class="inline" @click="toggleDictation('answer')">{{ speech.listening.value ? '停止语音输入' : '语音输入' }}</button></div><button class="primary" @click="addGraphAnswer">保存回答到证据链</button><el-select v-model="counterMode" aria-label="反事实场景" class="lab-mode-select"><el-option v-for="mode in ['技术面', '业务面', '压力面']" :key="mode" :label="mode" :value="mode" /></el-select><button class="inline" :disabled="working" @click="askCounterfactual">变更条件继续追问</button><button class="inline" :disabled="working || counterTurns.length < 2" @click="checkConsistency">对照多轮回答一致性</button></template>
        <template v-else-if="tab === 'replay'"><h2>能力进化实验室 · 同题重练</h2><p v-if="replayOrigin" class="record"><b>来自正式面试 · 首次评分 {{ replayOrigin.score ?? '—' }}</b><span>上次建议：{{ replayOrigin.feedback || '请结合原回答补充具体证据' }}</span></p><p class="lead">保留两次回答和反馈，聚焦行动、依据与表达结构的变化。</p><div class="field"><label>同一道题</label><textarea v-model="form.question" rows="3"></textarea></div><div class="comparison"><div class="field"><label>第一次回答</label><textarea v-model="form.first" rows="8"></textarea><button class="inline" @click="toggleDictation('first')">{{ speech.listening.value ? '停止语音输入' : '语音输入' }}</button></div><div class="field"><label>重练后回答</label><textarea v-model="form.second" rows="8"></textarea><button class="inline" @click="toggleDictation('second')">{{ speech.listening.value ? '停止语音输入' : '语音输入' }}</button></div></div><div class="comparison rubric"><div><b>原回答 · 表达覆盖 {{ firstScore.score }}/100</b><p v-for="item in firstScore.detail" :key="item.label">{{ item.hit ? '✓' : '○' }} {{ item.label }} <small v-if="item.hit">（命中“{{ item.basis }}”）</small></p></div><div><b>重练回答 · 表达覆盖 {{ secondScore.score }}/100</b><p v-for="item in secondScore.detail" :key="item.label">{{ item.hit ? '✓' : '○' }} {{ item.label }} <small v-if="item.hit">（命中“{{ item.basis }}”）</small></p></div></div><p class="muted">依据：仅检查是否明确写出个人行动、材料、结果和复盘词句；模型反馈与正式面试评分另行展示，不能把关键词计数当作能力分。</p><button class="primary" :disabled="working" @click="compareReplay">并排复盘并保存</button><p class="muted">若要更新简历，请回到简历工坊人工采纳，不会自动改写经历事实。</p></template>
        <div v-if="aiOutput" class="coach-output"><div><b>{{ aiSource === 'model' ? 'AI 教练建议' : '本地练习提示 · 尚未接入模型' }}</b><button class="inline" @click="speak(aiOutput)">朗读</button><button v-if="spoken" class="inline" @click="stopSpeech">停止播报</button></div><p>{{ aiOutput }}</p></div>
        <section v-if="activePractice.length" class="history"><h3>本模块练习记录</h3><article v-for="p in activePractice.slice(0, 8)" :key="p.id" class="record"><small>{{ new Date(p.createdAt).toLocaleString() }}</small><b>{{ p.question || '练习记录' }}</b><p v-if="p.first">第一次：{{ p.first }}</p><p v-if="p.second">第二次：{{ p.second }}</p><p v-if="p.answer">我的回答：{{ p.answer }}</p><p v-if="p.firstScore && p.secondScore">同口径表达覆盖：{{ p.firstScore.score }} → {{ p.secondScore.score }} / 100</p><p v-if="p.feedback">反馈：{{ p.feedback }}</p></article></section>
      </main>
    </div>
  </div>
</template>

<style scoped>
.lab{max-width:1380px;margin:auto;padding:28px 30px 80px;color:var(--ink)}.lab-hero{padding:32px 34px;background:var(--surface);border:1px solid var(--line);border-radius:20px;position:relative}.eyebrow{font:700 11px ui-monospace,monospace;letter-spacing:.16em;color:var(--accent)}h1{font-size:clamp(28px,3vw,42px);line-height:1.25;margin:16px 0}h2{font-size:25px;margin:0 0 10px}h3{margin:28px 0 14px}.lab-hero p,.lead{color:var(--muted);line-height:1.8}.save-hint{font-size:12px;color:var(--muted)}.lab-notice{margin-top:16px;padding:14px 20px;background:var(--surface-2);border-radius:12px}.lab-notice a{color:var(--accent)}.lab-layout{display:grid;grid-template-columns:245px minmax(0,1fr);gap:22px;margin-top:22px}.lab-nav{align-self:start;display:grid;gap:7px;position:sticky;top:24px}.lab-nav button{border:1px solid var(--line);background:var(--surface);color:var(--ink);text-align:left;padding:13px 16px;border-radius:12px;cursor:pointer}.lab-nav button.active{border-color:var(--accent);box-shadow:inset 3px 0 var(--accent)}.lab-nav strong,.lab-nav small{display:block}.lab-nav small{font-size:11px;color:var(--muted);margin-top:5px;line-height:1.5}.lab-panel{min-width:0;background:var(--surface);border:1px solid var(--line);border-radius:20px;padding:30px}.field{display:grid;gap:8px;margin:18px 0}.field label{font-weight:700;font-size:13px}.field textarea,.field input,.field select{width:100%;box-sizing:border-box;padding:12px 14px;border:1px solid var(--line);background:var(--surface-2);color:var(--ink);border-radius:10px;font:inherit;line-height:1.6}.field textarea:focus,.field input:focus{outline:2px solid var(--accent)}button.primary{background:var(--accent);color:white;border:0;border-radius:9px;padding:11px 17px;font-weight:700;cursor:pointer;margin:5px 8px 5px 0}button.inline{border:0;background:transparent;color:var(--accent);font-weight:700;cursor:pointer;padding:8px}button:disabled{opacity:.5;cursor:not-allowed}.chain-flow{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:16px;margin:18px 0}.chain-flow span{position:relative;padding:12px 8px;border:1px solid var(--accent);border-radius:8px;font-size:12px;text-align:center;overflow-wrap:anywhere}.chain-flow span:not(:last-child):after{content:"→";position:absolute;right:-15px;top:35%;color:var(--accent)}.graph-grid{display:grid;grid-template-columns:minmax(180px,1fr) minmax(260px,1.4fr);gap:20px}.node-list{display:grid;align-content:start;gap:9px}.node-list button{padding:12px;border:1px solid var(--line);background:var(--surface-2);color:var(--ink);border-radius:10px;cursor:pointer;text-align:left}.node-list button.selected{border-color:var(--accent)}.node-list span{display:block;font-size:11px;margin-bottom:5px}.found{color:#16866f}.gap{color:#bd6b25}.node-detail{border:1px solid var(--line);border-radius:12px;padding:16px;align-self:start}.node-detail h3{margin:3px 0 12px}.chain-title{color:var(--accent);font-weight:700;font-size:13px}.evidence,.record{border:1px solid var(--line);border-radius:11px;margin:12px 0;padding:14px;overflow-wrap:anywhere}.evidence b,.record b{display:block}.evidence blockquote{margin:12px 0;padding-left:12px;border-left:3px solid var(--accent);white-space:pre-wrap}.evidence small,.record small,.record span,.muted{color:var(--muted);font-size:12px}.record p,.coach-output p{line-height:1.7;white-space:pre-wrap;overflow-wrap:anywhere}.coach-output{margin-top:24px;padding:18px;border:1px solid var(--accent);border-radius:12px;background:var(--surface-2)}.coach-output>div{display:flex;gap:12px;align-items:center}.rubric{margin:12px 0;padding:12px;border:1px solid var(--line);border-radius:10px}.rubric p{font-size:12px;margin:5px 0}.comparison{display:grid;grid-template-columns:1fr 1fr;gap:16px}.history{margin-top:40px;border-top:1px solid var(--line)}.file-action{color:var(--accent);cursor:pointer;font-size:12px}.file-action input{width:auto;margin-left:8px} .lab .field :deep(.el-select) { width: 100%; min-width: 0; }
.lab-mode-select { width: 145px; margin: 8px 12px 8px 0; vertical-align: middle; }
.lab-nav-group { display: grid; gap: 7px; }
.lab-nav-title { padding: 12px 12px 2px; color: var(--muted); font-size: 11px; font-weight: 750; letter-spacing: .08em; }
.lab-steps { display: grid; gap: 14px; margin-top: 26px; }
.lab-steps button { display: grid; gap: 7px; padding: 19px 21px; text-align: left; border: 1px solid var(--line); border-radius: var(--radius-lg); background: var(--surface-2); color: var(--ink); cursor: pointer; }
.lab-steps button:hover, .lab-steps button:focus-visible { border-color: var(--accent); outline-color: var(--accent); }
.lab-steps span { color: var(--accent); font-weight: 750; font-size: 12px; }
.lab-steps strong { font-size: 18px; }
.lab-steps small { color: var(--ink-2); font-size: 13px; }
.lab-steps em { color: var(--accent); font-size: 12px; font-style: normal; font-weight: 700; }
.lab-next { margin-top: 22px; color: var(--muted); }
.lab-advanced { padding: 15px; border: 1px solid var(--line); border-radius: var(--radius-md); margin: 15px 0; }
.lab-advanced summary { cursor: pointer; font-weight: 700; color: var(--accent); }
@media(max-width:900px){.lab{padding:16px}.lab-layout{grid-template-columns:1fr}.lab-nav{position:static;display:flex;overflow-x:auto}.lab-nav button{min-width:180px}.lab-panel{padding:18px}}@media(max-width:620px){.graph-grid,.comparison,.chain-flow{grid-template-columns:1fr}.chain-flow span:not(:last-child):after{content:"↓";right:50%;top:auto;bottom:-16px}.lab-hero{padding:22px}}

.search-summary{margin:24px 0;padding:20px;border:1px solid var(--line);border-radius:var(--radius-lg);background:var(--surface-2)}
.search-heading{display:flex;justify-content:space-between;gap:16px;align-items:start}.search-heading h3{margin:0 0 6px}.search-heading p{margin:0 0 16px}
.search-error{padding:12px;border-radius:9px;background:rgba(189,107,37,.12);color:#a05a18;overflow-wrap:anywhere}
.recommend-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:12px;margin:14px 0}.recommend-card{padding:15px;border:1px solid var(--line);border-radius:12px;background:var(--surface)}
.recommend-card h4{margin:7px 0 14px}.recommend-card small{color:var(--accent)}.coverage-row{border-top:1px solid var(--line);padding:11px 0}.coverage-row b{font-size:13px}.coverage-row blockquote{margin:9px 0;padding-left:10px;border-left:2px solid var(--accent);font-size:12px;white-space:pre-wrap;overflow-wrap:anywhere}
.coverage-gap{padding:14px;border:1px dashed var(--line);border-radius:9px}.coverage-gap p{margin:7px 0 0;line-height:1.7}
@media(max-width:620px){.search-heading{flex-direction:column}.recommend-grid{grid-template-columns:1fr}}
</style>
