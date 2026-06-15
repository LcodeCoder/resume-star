/**
 * 麦克风录音组合式：采集并输出讯飞语音识别要求的 16k/16bit/单声道 raw PCM。
 *
 * 为什么不用 MediaRecorder：它只会吐 webm/opus 或 mp4，不是裸 PCM，讯飞无法直接吃。
 * 这里用 AudioContext + ScriptProcessorNode 自采 Float32 样本，停止时降采样到 16000Hz、
 * 转 Int16 PCM 并拼成 Blob。ScriptProcessorNode 虽已标记废弃，但在微信 X5 / 老安卓 /
 * iOS Safari 上兼容性最好（AudioWorklet 在这些环境支持参差）。
 *
 * 用于「沉浸式语音面试」在微信等不支持浏览器原生 SpeechRecognition 的环境下作答：
 * 录制整段上传后端 → 后端转讯飞 → 返回文字。免手操（自动断句）需要 VAD，本组合式暂不提供。
 */
import { ref } from 'vue'

const TARGET_RATE = 16000 // 讯飞要求 16kHz

export function useRecorder() {
  const supported =
    typeof navigator !== 'undefined' &&
    !!navigator.mediaDevices &&
    !!navigator.mediaDevices.getUserMedia &&
    !!(window.AudioContext || window.webkitAudioContext)

  const recording = ref(false)
  // 录音实时音量（0~1），用于波形动画
  const volume = ref(0)

  let audioCtx = null
  let micStream = null
  let source = null
  let processor = null
  let sourceRate = TARGET_RATE
  let chunks = [] // Float32Array[]，累积原始样本

  /**
   * 开始录音。需在安全上下文（https/localhost）且用户授权麦克风。
   * @returns {Promise<boolean>} 是否成功开始
   */
  const start = async () => {
    if (!supported) return false
    try {
      micStream = await navigator.mediaDevices.getUserMedia({
        audio: { channelCount: 1, echoCancellation: true, noiseSuppression: true }
      })
      const Ctx = window.AudioContext || window.webkitAudioContext
      audioCtx = new Ctx()
      sourceRate = audioCtx.sampleRate // 设备实际采样率（常见 44100/48000），停止时再降采样
      source = audioCtx.createMediaStreamSource(micStream)
      processor = audioCtx.createScriptProcessor(4096, 1, 1)
      chunks = []
      processor.onaudioprocess = (e) => {
        const input = e.inputBuffer.getChannelData(0)
        // 拷贝一份存起来（input 是复用缓冲，不能直接 push）
        chunks.push(new Float32Array(input))
        // 顺带算音量做波形
        let sum = 0
        for (let i = 0; i < input.length; i++) sum += input[i] * input[i]
        volume.value = Math.min(1, Math.sqrt(sum / input.length) * 3)
      }
      source.connect(processor)
      processor.connect(audioCtx.destination)
      recording.value = true
      return true
    } catch (e) {
      cleanup()
      recording.value = false
      return false
    }
  }

  /**
   * 停止录音，返回 16k/16bit/单声道 raw PCM 的 Blob。
   * @returns {Blob|null} PCM 数据；无有效录音时返回 null
   */
  const stop = () => {
    if (!recording.value) return null
    recording.value = false
    const merged = mergeChunks(chunks)
    cleanup()
    if (!merged || merged.length === 0) return null
    const down = downsample(merged, sourceRate, TARGET_RATE)
    const pcm = floatToPcm16(down)
    return new Blob([pcm], { type: 'application/octet-stream' })
  }

  /** 中断录音并丢弃数据（不返回结果） */
  const cancel = () => {
    recording.value = false
    chunks = []
    cleanup()
  }

  const cleanup = () => {
    if (processor) { try { processor.disconnect() } catch (e) { /* ignore */ } processor.onaudioprocess = null; processor = null }
    if (source) { try { source.disconnect() } catch (e) { /* ignore */ } source = null }
    if (micStream) { micStream.getTracks().forEach((t) => { try { t.stop() } catch (e) { /* ignore */ } }); micStream = null }
    if (audioCtx) { try { audioCtx.close() } catch (e) { /* ignore */ } audioCtx = null }
    volume.value = 0
  }

  return { supported, recording, volume, start, stop, cancel }
}

/** 把多块 Float32Array 拼成一整块 */
function mergeChunks(chunks) {
  let total = 0
  for (const c of chunks) total += c.length
  const out = new Float32Array(total)
  let offset = 0
  for (const c of chunks) { out.set(c, offset); offset += c.length }
  return out
}

/** 线性插值降采样：从设备采样率降到 16000Hz */
function downsample(buffer, fromRate, toRate) {
  if (toRate === fromRate) return buffer
  const ratio = fromRate / toRate
  const newLen = Math.round(buffer.length / ratio)
  const result = new Float32Array(newLen)
  let pos = 0
  for (let i = 0; i < newLen; i++) {
    const idx = i * ratio
    const i0 = Math.floor(idx)
    const i1 = Math.min(i0 + 1, buffer.length - 1)
    const frac = idx - i0
    result[i] = buffer[i0] * (1 - frac) + buffer[i1] * frac
    pos = i1
  }
  void pos
  return result
}

/** Float32 [-1,1] → Int16 小端 PCM */
function floatToPcm16(floats) {
  const out = new DataView(new ArrayBuffer(floats.length * 2))
  for (let i = 0; i < floats.length; i++) {
    let s = Math.max(-1, Math.min(1, floats[i]))
    s = s < 0 ? s * 0x8000 : s * 0x7fff
    out.setInt16(i * 2, s, true) // 小端
  }
  return out.buffer
}
