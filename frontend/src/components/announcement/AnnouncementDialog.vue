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
  <el-dialog
    v-model="visible"
    :title="announcement?.title || '站内公告'"
    width="480px"
    align-center
    append-to-body
    :close-on-click-modal="false"
    class="announcement-dialog"
  >
    <div class="announcement-body">
      <p v-for="(line, idx) in paragraphs(announcement?.content)" :key="idx" class="announcement-line">{{ line }}</p>
    </div>
    <template #footer>
      <el-button type="primary" @click="acknowledge">我知道了</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
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

:global(html.dark) .announcement-line {
  color: var(--color-text-tertiary);
}
</style>
