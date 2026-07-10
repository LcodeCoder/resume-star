<!--
  站内公告弹窗组件
  页面位置：进站后由主布局挂载，自动拉取当前启用公告并弹出
  功能：展示后台维护的最新公告；用户「我知道了」后按 id + updateTime 版本指纹记到 localStorage，
        同一版本不再弹；公告内容更新（updateTime 变化）则视为新版本，会再次弹出。
-->
<script setup>
import { onMounted, ref } from 'vue'
import { getActiveAnnouncement } from '../../api/announcement'

/** 已读公告版本指纹本地存储键 */
const READ_KEY = 'resume_lcode_announcement_read'

const visible = ref(false)
const announcement = ref(null)

/** 公告版本指纹：id + 更新时间，内容更新后指纹变化即重新弹出 */
const fingerprintOf = (item) => `${item.id}@${item.updateTime || item.createTime || ''}`

/** 读取本地已读指纹集合 */
const readFingerprints = () => {
  try {
    const raw = localStorage.getItem(READ_KEY)
    return raw ? JSON.parse(raw) : []
  } catch (e) {
    return []
  }
}

onMounted(async () => {
  let active = null
  try {
    active = await getActiveAnnouncement()
  } catch (e) {
    return
  }
  if (!active || !active.id) return
  // 已读过当前版本则不再弹
  if (readFingerprints().includes(fingerprintOf(active))) return
  announcement.value = active
  visible.value = true
})

/** 关闭并记录已读版本指纹（保留最近 20 条，避免无限增长） */
const acknowledge = () => {
  if (announcement.value) {
    const list = readFingerprints()
    const fp = fingerprintOf(announcement.value)
    if (!list.includes(fp)) {
      list.push(fp)
      localStorage.setItem(READ_KEY, JSON.stringify(list.slice(-20)))
    }
  }
  visible.value = false
}

/** 公告正文按换行拆分为段落 */
const paragraphs = (content) => String(content || '').split('\n').filter((line) => line.trim())
</script>

<template>
  <transition name="signal-panel">
    <aside v-if="visible" class="announcement-signal" role="status" aria-live="polite">
      <header><span><i></i> 站内信号</span><button type="button" aria-label="关闭公告" @click="acknowledge">×</button></header>
      <h2>{{ announcement?.title || '站内公告' }}</h2>
      <div class="announcement-body">
        <p v-for="(line, idx) in paragraphs(announcement?.content)" :key="idx" class="announcement-line">{{ line }}</p>
      </div>
      <button class="announcement-action" type="button" @click="acknowledge">标记为已读</button>
    </aside>
  </transition>
</template>

<style scoped>
.announcement-signal { position: fixed; z-index: var(--z-toast, 90); right: 22px; top: 82px; width: min(360px, calc(100vw - 28px)); padding: 16px; border: 1px solid var(--line); border-radius: var(--radius-md); background: var(--surface); color: var(--ink); box-shadow: var(--shadow); }
.announcement-signal header { display: flex; align-items: center; justify-content: space-between; color: var(--muted); font-size: 11px; }.announcement-signal header span { display: flex; align-items: center; gap: 7px; }.announcement-signal header i { width: 7px; height: 7px; border-radius: 50%; background: var(--success); }.announcement-signal header button { width: 28px; height: 28px; border: 0; background: transparent; color: var(--muted); font-size: 20px; cursor: pointer; }.announcement-signal h2 { margin: 12px 0 8px; font-size: 16px; }
.announcement-body {
  max-height: 220px;
  overflow-y: auto;
}
.announcement-line {
  font-size: 14px;
  line-height: 1.9;
  color: var(--ink-2);
  margin: 0 0 8px;
}
.announcement-line:last-child {
  margin-bottom: 0;
}

.announcement-action { margin-top: 10px; padding: 7px 0; border: 0; background: transparent; color: var(--accent); font-weight: 700; cursor: pointer; }
.signal-panel-enter-active, .signal-panel-leave-active { transition: opacity 180ms ease, transform 180ms cubic-bezier(.16,1,.3,1); }.signal-panel-enter-from, .signal-panel-leave-to { opacity: 0; transform: translateY(-8px); }
@media(max-width:600px){.announcement-signal{right:14px;top:66px}}
</style>
