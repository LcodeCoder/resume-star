/**
 * 语音能力组合式（浏览器原生 Web Speech API，免费、无需后端/密钥）
 *
 * - 文字转语音 TTS：window.speechSynthesis + SpeechSynthesisUtterance
 * - 语音转文字 STT：window.SpeechRecognition / webkitSpeechRecognition（Chrome/Edge 支持最佳）
 *
 * 用于「沉浸式语音面试」：AI 问题用 TTS 念出来，候选人用 STT 语音作答。
 * 不支持的浏览器会通过 ttsSupported / sttSupported 暴露出来，由调用方降级到纯文字。
 */
import { ref, shallowRef } from 'vue'

export function useSpeech() {
  const synth = typeof window !== 'undefined' ? window.speechSynthesis : null
  const SpeechRecognition =
    typeof window !== 'undefined' ? (window.SpeechRecognition || window.webkitSpeechRecognition) : null

  const ttsSupported = !!synth
  const sttSupported = !!SpeechRecognition
  // 移动端原生识别有诸多坑：不认 continuous（说一句自停）、与第二路 getUserMedia 抢麦、
  // 重启易抛 InvalidStateError。下面针对移动端做适配：跳过音量计、自动续听、干净重启。
  const isMobile = typeof navigator !== 'undefined' &&
    /Android|iPhone|iPad|iPod|Mobile|HarmonyOS|MicroMessenger/i.test(navigator.userAgent || '')

  const speaking = ref(false)
  const listening = ref(false)
  // 识别到的实时文字：finalText 为已确定段，interimText 为正在识别的临时段
  const finalText = ref('')
  const interimText = ref('')
  // 录音实时音量（0~1），用于波形动画；取不到麦克风音量时恒为 0，由 UI 回退到动画条
  const volume = ref(0)

  const voices = ref([])
  const selectedVoiceURI = ref('')

  const STORAGE_KEY = 'interview_tts_voice'

  /** 仅中文音色（用于下拉选择） */
  const zhVoices = ref([])

  /**
   * 给中文音色打分，分越高越自然：
   * - Chrome 的 "Google 普通话" 是云端神经网络音色，最自然 → 最高优先
   * - 网络音色（localService=false）通常优于本地合成
   * - macOS 增强/高级音色（Meijia/Sinji/Yue 等）优于老的 Ting-Ting
   */
  const rankVoice = (v) => {
    let score = 0
    const name = (v.name || '') + ' ' + (v.voiceURI || '')
    if (/google/i.test(name)) score += 100
    if (v.localService === false) score += 40
    if (/neural|premium|enhanced|增强|高级/i.test(name)) score += 30
    if (/meijia|sinji|yue|tingting|美佳|婷婷|语音/i.test(name)) score += 10
    if (/zh[-_]?CN|cmn|普通话|简体/i.test((v.lang || '') + name)) score += 5
    return score
  }

  /** 加载系统可用音色，挑选最自然的中文音色作为默认 */
  const loadVoices = () => {
    if (!synth) return
    const list = synth.getVoices() || []
    voices.value = list
    const zh = list
      .filter((v) => /zh|cmn|Chinese|普通话/i.test((v.lang || '') + (v.name || '')))
      .sort((a, b) => rankVoice(b) - rankVoice(a))
    zhVoices.value = zh
    // 恢复上次选择；否则用评分最高的中文音色
    const saved = (() => { try { return localStorage.getItem(STORAGE_KEY) } catch (e) { return null } })()
    if (saved && list.some((v) => v.voiceURI === saved)) {
      selectedVoiceURI.value = saved
    } else if (!selectedVoiceURI.value && zh.length) {
      selectedVoiceURI.value = zh[0].voiceURI
    }
  }
  if (synth) {
    loadVoices()
    // 部分浏览器音色异步加载，需监听 voiceschanged
    synth.onvoiceschanged = loadVoices
  }

  /** 切换音色并记住选择 */
  const setVoice = (uri) => {
    selectedVoiceURI.value = uri
    try { localStorage.setItem(STORAGE_KEY, uri) } catch (e) { /* ignore */ }
  }

  /**
   * 朗读一段文字。返回 Promise，朗读结束（或失败/被打断）后 resolve。
   * @param {string} text 要朗读的文字
   * @param {{rate?:number,pitch?:number,onstart?:Function}} opts
   */
  const speak = (text, opts = {}) => {
    return new Promise((resolve) => {
      if (!synth || !text) {
        resolve()
        return
      }
      // 打断上一段，避免叠音
      synth.cancel()
      const u = new SpeechSynthesisUtterance(text)
      u.lang = 'zh-CN'
      u.rate = opts.rate ?? 0.96
      u.pitch = opts.pitch ?? 1.02
      const voice = voices.value.find((v) => v.voiceURI === selectedVoiceURI.value)
      if (voice) u.voice = voice
      u.onstart = () => {
        speaking.value = true
        opts.onstart?.()
      }
      u.onend = () => {
        speaking.value = false
        resolve()
      }
      u.onerror = () => {
        speaking.value = false
        resolve()
      }
      synth.speak(u)
    })
  }

  /** 立即停止朗读 */
  const stopSpeaking = () => {
    if (synth) synth.cancel()
    speaking.value = false
  }

  const recognition = shallowRef(null)
  let silenceTimer = null
  let stoppedByUs = false
  let keepAlive = false // 移动端「自动续听」：true 时 onend 后自动重启识别，模拟 continuous

  // ===== 麦克风音量计（用于录音波形）=====
  let audioCtx = null
  let micStream = null
  let analyser = null
  let meterRaf = 0

  const startMeter = async () => {
    try {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) return
      micStream = await navigator.mediaDevices.getUserMedia({ audio: true })
      const Ctx = window.AudioContext || window.webkitAudioContext
      if (!Ctx) return
      audioCtx = new Ctx()
      const source = audioCtx.createMediaStreamSource(micStream)
      analyser = audioCtx.createAnalyser()
      analyser.fftSize = 512
      source.connect(analyser)
      const buf = new Uint8Array(analyser.frequencyBinCount)
      const tick = () => {
        if (!analyser) return
        analyser.getByteTimeDomainData(buf)
        // 计算 RMS，归一化到 0~1
        let sum = 0
        for (let i = 0; i < buf.length; i++) {
          const v = (buf[i] - 128) / 128
          sum += v * v
        }
        volume.value = Math.min(1, Math.sqrt(sum / buf.length) * 3)
        meterRaf = requestAnimationFrame(tick)
      }
      tick()
    } catch (e) {
      // 取不到麦克风音量（权限/不支持）→ 静默，UI 回退到动画条
      volume.value = 0
    }
  }

  const stopMeter = () => {
    if (meterRaf) cancelAnimationFrame(meterRaf)
    meterRaf = 0
    analyser = null
    if (micStream) {
      micStream.getTracks().forEach((t) => { try { t.stop() } catch (e) { /* ignore */ } })
      micStream = null
    }
    if (audioCtx) {
      try { audioCtx.close() } catch (e) { /* ignore */ }
      audioCtx = null
    }
    volume.value = 0
  }

  const clearSilence = () => {
    if (silenceTimer) { clearTimeout(silenceTimer); silenceTimer = null }
  }

  /**
   * 开始语音识别。识别到的文字会持续写入 finalText / interimText。
   * @param {{onfinal?:(text:string)=>void, onsilence?:(text:string)=>void, silenceMs?:number, onerror?:(err:string)=>void}} opts
   *  - onsilence：用户停顿 silenceMs 毫秒后自动触发（免手操模式据此自动提交）
   *  - onerror：识别致命出错（无授权/占用/网络）时回调，便于上层提示或建议切云端
   */
  const startListening = (opts = {}) => {
    if (!SpeechRecognition) return false
    // 录音时先停掉朗读，避免麦克风录进面试官的声音
    stopSpeaking()
    // 干净重启：先彻底中断上一段识别，避免移动端 rec.start() 抛 InvalidStateError 导致「点了不开麦」
    if (recognition.value) {
      try { recognition.value.onend = null; recognition.value.abort() } catch (e) { /* ignore */ }
      recognition.value = null
    }
    // 移动端手动模式（无 silenceMs）开启自动续听：原生说一句就自停，自动重启以模拟 continuous
    const wantContinuous = isMobile && !opts.silenceMs

    const armSilence = () => {
      if (!opts.silenceMs || !opts.onsilence) return
      clearSilence()
      silenceTimer = setTimeout(() => {
        const text = stopListening()
        opts.onsilence(text)
      }, opts.silenceMs)
    }

    // 创建一个识别实例并绑定回调（自动续听时可能多次创建，finalText 跨实例累加不清空）
    const buildRec = () => {
      const rec = new SpeechRecognition()
      rec.lang = 'zh-CN'
      rec.continuous = true
      rec.interimResults = true
      rec.onresult = (event) => {
        let interim = ''
        for (let i = event.resultIndex; i < event.results.length; i++) {
          const res = event.results[i]
          if (res.isFinal) {
            finalText.value += res[0].transcript
            opts.onfinal?.(finalText.value)
          } else {
            interim += res[0].transcript
          }
        }
        interimText.value = interim
        armSilence() // 每次有声音就重置静音计时
      }
      rec.onend = () => {
        interimText.value = ''
        // 自动续听：移动端非用户主动停止时重启识别（同实例失败则新建），finalText 保留累加
        if (keepAlive && !stoppedByUs) {
          try { rec.start(); return } catch (e) { /* 落到新建 */ }
          try { recognition.value = buildRec(); recognition.value.start(); return } catch (e) { /* ignore */ }
        }
        listening.value = false
        clearSilence()
        stopMeter()
      }
      rec.onerror = (event) => {
        const err = (event && event.error) || 'unknown'
        // 致命错误（无授权 / 麦克风被占 / 网络不可达）：停止续听并上抛，让上层提示或建议切云端
        if (['not-allowed', 'service-not-allowed', 'audio-capture', 'network'].includes(err)) {
          keepAlive = false
          interimText.value = ''
          clearSilence()
          stopMeter()
          listening.value = false
          opts.onerror?.(err)
        }
        // 其余（no-speech / aborted）：交给 onend 决定是否续听，不打断流程
      }
      return rec
    }

    const rec = buildRec()
    recognition.value = rec
    finalText.value = ''
    interimText.value = ''
    stoppedByUs = false
    keepAlive = wantContinuous
    try {
      rec.start()
      listening.value = true
      // 移动端跳过第二路 getUserMedia 音量计：它会与识别抢麦，导致「还没说话就停」；波形回退到动画条
      if (!isMobile) startMeter()
      armSilence() // 初始也起一个计时，避免用户一直不说话时卡住
      return true
    } catch (e) {
      keepAlive = false
      listening.value = false
      return false
    }
  }

  /** 停止识别，返回本次识别到的完整文字 */
  const stopListening = () => {
    keepAlive = false // 关掉自动续听，避免 onend 又重启
    clearSilence()
    stopMeter()
    stoppedByUs = true
    const rec = recognition.value
    if (rec) {
      try {
        rec.stop()
      } catch (e) {
        /* ignore */
      }
    }
    listening.value = false
    return (finalText.value + interimText.value).trim()
  }

  /** 组件卸载时清理：停止朗读与识别 */
  const dispose = () => {
    keepAlive = false
    stoppedByUs = true
    stopSpeaking()
    clearSilence()
    stopMeter()
    if (recognition.value) {
      try {
        recognition.value.onend = null
        recognition.value.abort()
      } catch (e) {
        /* ignore */
      }
    }
  }

  return {
    ttsSupported,
    sttSupported,
    speaking,
    listening,
    finalText,
    interimText,
    volume,
    voices,
    zhVoices,
    selectedVoiceURI,
    setVoice,
    speak,
    stopSpeaking,
    startListening,
    stopListening,
    dispose
  }
}
