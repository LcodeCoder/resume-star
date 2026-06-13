<!--
  后台公告管理 Tab
  功能：维护站内公告（新增 / 编辑 / 启用停用 / 删除）。启用中的最新一条会下发给用户端进站弹窗，
        用户按 id + updateTime 版本指纹「已读不再弹」，内容更新后会重新提醒。
-->
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAnnouncements, saveAnnouncement, deleteAnnouncement } from '../../api/admin'

const announcements = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref({ id: null, title: '', content: '', enabled: true })

const refresh = async () => {
  announcements.value = await listAnnouncements() || []
}

onMounted(refresh)

const openCreate = () => {
  form.value = { id: null, title: '', content: '', enabled: true }
  dialogVisible.value = true
}

const openEdit = (row) => {
  form.value = { id: row.id, title: row.title, content: row.content, enabled: row.enabled !== false }
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请填写公告标题')
    return
  }
  saving.value = true
  try {
    await saveAnnouncement({
      id: form.value.id,
      title: form.value.title.trim(),
      content: form.value.content,
      enabled: form.value.enabled
    })
    dialogVisible.value = false
    await refresh()
    ElMessage.success(form.value.id ? '公告已更新' : '公告已发布')
  } finally {
    saving.value = false
  }
}

/** 快速切换启用状态（行内开关） */
const toggleEnabled = async (row, enabled) => {
  await saveAnnouncement({ id: row.id, title: row.title, content: row.content, enabled })
  row.enabled = enabled
  ElMessage.success(enabled ? '已启用' : '已停用')
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除确认', { type: 'warning' })
  await deleteAnnouncement(row.id)
  await refresh()
  ElMessage.success('公告已删除')
}

/** 时间格式化：去掉 T 并截断到秒 */
const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')
</script>

<template>
  <section class="admin-panel-card card">
    <div class="admin-section-toolbar">
      <div>
        <h3>站内公告</h3>
        <p>启用中的最新一条公告会在用户进站时弹窗展示，更新内容后用户会重新看到。</p>
      </div>
      <el-button type="primary" @click="openCreate">发布公告</el-button>
    </div>

    <el-table :data="announcements" stripe>
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-switch
            :model-value="row.enabled !== false"
            active-text="启用"
            inactive-text="停用"
            @change="(value) => toggleEnabled(row, value)"
          />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="180">
        <template #default="{ row }">{{ fmtTime(row.updateTime || row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!announcements.length" description="暂无公告" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '发布公告'" width="560px">
      <el-form label-width="72px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="公告标题" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="支持多段文本，换行会按段落展示"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" active-text="发布后立即弹窗" inactive-text="暂不展示" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
</style>
