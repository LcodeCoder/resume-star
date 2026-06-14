<!--
  通知铃铛组件
  功能：拉取当前启用的站内公告作为通知项，展示未读红点；点击铃铛弹出通知列表，
        点击某条通知弹出公告详情弹窗。已读状态与进站自动弹窗共用 localStorage 版本指纹，
        任一处「已读」后红点即清除，不会重复打扰。
-->
<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElBadge, ElPopover, ElEmpty, ElButton, ElDialog } from 'element-plus'
import { getActiveAnnouncement } from '../../api/announcement'

/** 已读公告版本指纹本地存储键（与 AnnouncementDialog 共用，保持已读状态一致） */
const READ_KEY = 'resume_lcode_announcement_read'

/** 公告版本指纹：id + 更新时间，内容更新后指纹变化即视为新通知 */
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

/** 写入已读指纹（保留最近 20 条，避免无限增长） */
const persistRead = (fp) => {
  const list = readFingerprints()
  if (!list.includes(fp)) {
    list.push(fp)
    localStorage.setItem(READ_KEY, JSON.stringify(list.slice(-20)))
  }
}

const notifications = ref([])
const unreadCount = computed(() => notifications.value.filter((n) => !n.read).length)

const popoverVisible = ref(false)
const detailVisible = ref(false)
const current = ref(null)

/** 公告正文按换行拆分为段落 */
const paragraphs = (content) => String(content || '').split('\n').filter((line) => line.trim())

/** 相对时间展示（简单友好化，无需引第三方库） */
const formatTime = (raw) => {
  if (!raw) return ''
  const time = new Date(String(raw).replace(/-/g, '/')).getTime()
  if (Number.isNaN(time)) return ''
  const diff = Date.now() - time
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)} 分钟前`
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`
  if (diff < 30 * day) return `${Math.floor(diff / day)} 天前`
  return new Date(time).toLocaleDateString()
}

onMounted(async () => {
  let active = null
  try {
    active = await getActiveAnnouncement()
  } catch (e) {
    return
  }
  if (!active || !active.id) return
  const fp = fingerprintOf(active)
  notifications.value = [
    {
      id: active.id,
      fingerprint: fp,
      title: active.title || '站内公告',
      content: active.content || '',
      time: formatTime(active.updateTime || active.createTime),
      read: readFingerprints().includes(fp)
    }
  ]
})

/** 点击某条通知：弹出详情并标记已读 */
const openDetail = (item) => {
  current.value = item
  detailVisible.value = true
  popoverVisible.value = false
  if (!item.read) {
    item.read = true
    persistRead(item.fingerprint)
  }
}

/** 标记全部已读 */
const markAllAsRead = () => {
  notifications.value.forEach((n) => {
    if (!n.read) {
      n.read = true
      persistRead(n.fingerprint)
    }
  })
}
</script>

<template>
  <ElPopover
    :visible="popoverVisible"
    placement="bottom-end"
    :width="320"
    trigger="click"
    popper-class="notification-popover"
    @update:visible="(val) => popoverVisible = val"
  >
    <template #reference>
      <div class="notification-bell">
        <ElBadge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
          <button class="bell-button" :class="{ 'has-unread': unreadCount > 0 }" aria-label="通知">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/>
            </svg>
          </button>
        </ElBadge>
      </div>
    </template>

    <div class="notification-panel">
      <div class="notification-header">
        <span class="notification-title">通知</span>
        <ElButton v-if="unreadCount > 0" link type="primary" size="small" @click="markAllAsRead">
          全部已读
        </ElButton>
      </div>

      <div class="notification-list">
        <ElEmpty v-if="notifications.length === 0" description="暂无通知" :image-size="80" />

        <div
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="{ unread: !item.read }"
          @click="openDetail(item)"
        >
          <span v-if="!item.read" class="notification-item-dot"></span>
          <div class="notification-item-header">
            <span class="notification-item-title">{{ item.title }}</span>
            <span class="notification-item-time">{{ item.time }}</span>
          </div>
          <div class="notification-item-content">{{ item.content }}</div>
        </div>
      </div>
    </div>
  </ElPopover>

  <!-- 公告详情弹窗 -->
  <ElDialog
    v-model="detailVisible"
    :title="current?.title || '站内公告'"
    width="480px"
    align-center
    append-to-body
    class="announcement-dialog"
  >
    <div class="announcement-body">
      <p v-for="(line, idx) in paragraphs(current?.content)" :key="idx" class="announcement-line">{{ line }}</p>
    </div>
    <template #footer>
      <ElButton type="primary" @click="detailVisible = false">我知道了</ElButton>
    </template>
  </ElDialog>
</template>

<style scoped>
.notification-bell {
  display: flex;
  align-items: center;
}

.bell-button {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  background: rgba(91, 91, 214, 0.08);
  color: #5b5bd6;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.15s ease, transform 0.15s ease;
}

.bell-button:hover {
  background: rgba(91, 91, 214, 0.16);
  transform: scale(1.05);
}

.bell-button.has-unread {
  animation: bell-shake 2.4s ease-in-out infinite;
}

@keyframes bell-shake {
  0%, 100% { transform: rotate(0deg); }
  10%, 30% { transform: rotate(-10deg); }
  20%, 40% { transform: rotate(10deg); }
  50%, 90% { transform: rotate(0deg); }
}

.notification-panel {
  min-height: 80px;
  max-height: 420px;
  display: flex;
  flex-direction: column;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 4px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.notification-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

.notification-list {
  flex: 1;
  overflow-y: auto;
  margin: 0 -12px -12px;
}

.notification-item {
  position: relative;
  padding: 12px 16px 12px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background: #f5f5f7;
}

.notification-item.unread {
  background: rgba(91, 91, 214, 0.05);
}

.notification-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.notification-item-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-item-time {
  font-size: 12px;
  color: #9e9ea4;
  flex-shrink: 0;
}

.notification-item-content {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-item-dot {
  position: absolute;
  top: 17px;
  left: 10px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #5b5bd6;
}

.announcement-body {
  max-height: 50vh;
  overflow-y: auto;
}

.announcement-line {
  font-size: 14px;
  line-height: 1.9;
  color: var(--color-text-tertiary);
  margin: 0 0 8px;
}

.announcement-line:last-child {
  margin-bottom: 0;
}

/* ===== 暗色模式：分隔线 / 悬停 / 时间 ===== */
html.dark .notification-header,
html.dark .notification-item {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

html.dark .notification-item:hover {
  background: var(--color-elevated);
}

html.dark .notification-item.unread {
  background: rgba(124, 130, 245, 0.12);
}

html.dark .notification-item-time {
  color: var(--color-text-muted);
}
</style>
