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

  const speaking = ref(false)
  const listening = ref(false)
  // 识别到的实时文字：finalText 为已确定段，interimText 为正在识别的临时段
  const finalText = ref('')
  const interimText = ref('')

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

  /**
   * 开始语音识别。识别到的文字会持续写入 finalText / interimText。
   * @param {{onfinal?:(text:string)=>void}} opts
   */
  const startListening = (opts = {}) => {
    if (!SpeechRecognition) return false
    // 录音时先停掉朗读，避免麦克风录进面试官的声音
    stopSpeaking()
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
    }
    rec.onend = () => {
      listening.value = false
      interimText.value = ''
    }
    rec.onerror = () => {
      listening.value = false
      interimText.value = ''
    }
    recognition.value = rec
    finalText.value = ''
    interimText.value = ''
    try {
      rec.start()
      listening.value = true
      return true
    } catch (e) {
      listening.value = false
      return false
    }
  }

  /** 停止识别，返回本次识别到的完整文字 */
  const stopListening = () => {
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
    stopSpeaking()
    if (recognition.value) {
      try {
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
