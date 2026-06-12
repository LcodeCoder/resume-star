<!--
  后台 AI 配置 Tab
  功能：管理 AI 接口配置，保留现有保存、启用、删除流程
-->
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAiConfig, enableAiConfig, listAiConfigs, saveAiConfig } from '../../api/aiConfig'

const aiConfigs = ref([])
const showAiDialog = ref(false)
const aiForm = reactive({
  id: null,
  name: '',
  endpoint: '',
  apiKey: '',
  model: '',
  timeoutMillis: 15000,
  enabled: 1
})

const refresh = async () => {
  aiConfigs.value = await listAiConfigs()
}

onMounted(refresh)

const openAiDialog = (config = null) => {
  if (config) Object.assign(aiForm, config)
  else {
    aiForm.id = null
    aiForm.name = ''
    aiForm.endpoint = ''
    aiForm.apiKey = ''
    aiForm.model = ''
    aiForm.timeoutMillis = 15000
    aiForm.enabled = 1
  }
  showAiDialog.value = true
}

const handleSaveAi = async () => {
  if (!aiForm.name || !aiForm.endpoint || !aiForm.apiKey || !aiForm.model) {
    ElMessage.warning('请填写完整信息')
    return
  }
  await saveAiConfig({ ...aiForm })
  ElMessage.success('保存成功')
  showAiDialog.value = false
  await refresh()
}

const handleDeleteAi = async (item) => {
  await ElMessageBox.confirm(`确定删除配置「${item.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteAiConfig(item.id)
  ElMessage.success('已删除')
  await refresh()
}

const handleEnableAi = async (item) => {
  await enableAiConfig(item.id)
  ElMessage.success(`已启用「${item.name}」`)
  await refresh()
}
</script>

<template>
  <section class="admin-panel-card card">
    <div class="admin-section-toolbar">
      <div>
        <h3>AI 接口配置</h3>
        <p>配置模型网关、API Key 和超时时间，供后端统一代理调用。</p>
      </div>
      <el-button type="primary" @click="openAiDialog()">新增配置</el-button>
    </div>

    <el-table :data="aiConfigs" stripe>
      <el-table-column prop="name" label="配置名称" width="150" />
      <el-table-column prop="endpoint" label="接口地址" min-width="220" show-overflow-tooltip />
      <el-table-column prop="apiKey" label="API Key" width="180" show-overflow-tooltip />
      <el-table-column prop="model" label="模型" width="150" />
      <el-table-column prop="timeoutMillis" label="超时(ms)" width="100" />
      <el-table-column prop="enabled" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
            {{ row.enabled === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.enabled !== 1" size="small" type="success" @click="handleEnableAi(row)">启用</el-button>
          <el-button size="small" @click="openAiDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="handleDeleteAi(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>

  <el-dialog v-model="showAiDialog" :title="aiForm.id ? '编辑 AI 配置' : '新增 AI 配置'" width="600px">
    <el-form label-position="top">
      <el-form-item label="配置名称" required>
        <el-input v-model="aiForm.name" placeholder="如：OpenAI GPT-4" />
      </el-form-item>
      <el-form-item label="接口地址" required>
        <el-input v-model="aiForm.endpoint" placeholder="https://api.openai.com/v1/chat/completions" />
      </el-form-item>
      <el-form-item label="API Key" required>
        <el-input v-model="aiForm.apiKey" type="password" placeholder="sk-xxx" show-password />
      </el-form-item>
      <el-form-item label="模型名称" required>
        <el-input v-model="aiForm.model" placeholder="gpt-4" />
      </el-form-item>
      <el-form-item label="超时时间（毫秒）">
        <el-input-number v-model="aiForm.timeoutMillis" :min="1000" :max="60000" :step="1000" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showAiDialog = false">取消</el-button>
      <el-button type="primary" @click="handleSaveAi">保存</el-button>
    </template>
  </el-dialog>
</template>
