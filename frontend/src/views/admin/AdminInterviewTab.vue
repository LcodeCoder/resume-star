<!--
  后台模拟测试管理 Tab
  功能：
    1) 面试分类增删改查（与模板分类逻辑一致）
    2) 模拟面试全局配置：总时长、最大题数、每日发起上限、开场白、系统 Prompt
-->
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminInterviewCategories,
  createInterviewCategory,
  updateInterviewCategory,
  deleteInterviewCategory
} from '../../api/adminInterview'
import { getSystemConfig, updateSystemConfig } from '../../api/systemConfig'

const activeSub = ref('config')
const categories = ref([])
const loading = ref(false)

const config = reactive({
  interviewTotalMinutes: 15,
  interviewMaxQuestions: 8,
  interviewDailyLimit: 3,
  interviewOpening: '',
  interviewSelfIntroPrompt: '',
  interviewSystemPrompt: '',
  interviewImmersiveEnabled: true,
  interviewImmersiveCost: 2,
  interviewImmersiveMinutes: 30,
  interviewTtsEnabled: true,
  interviewTtsKey: '',
  interviewTtsHd: false,
  interviewAsrEnabled: true,
  interviewAsrAppId: '',
  interviewAsrApiKey: '',
  interviewAsrApiSecret: ''
})

const loadCategories = async () => {
  categories.value = await getAdminInterviewCategories()
}

const loadConfig = async () => {
  const cfg = await getSystemConfig()
  Object.keys(config).forEach((k) => {
    if (cfg[k] !== undefined && cfg[k] !== null) config[k] = cfg[k]
  })
}

onMounted(async () => {
  await Promise.all([loadCategories(), loadConfig()])
})

const handleSaveConfig = async () => {
  if (!config.interviewTotalMinutes || config.interviewTotalMinutes < 1) {
    ElMessage.warning('总时长至少 1 分钟')
    return
  }
  if (!config.interviewMaxQuestions || config.interviewMaxQuestions < 1) {
    ElMessage.warning('最大题数至少 1 题')
    return
  }
  loading.value = true
  try {
    await updateSystemConfig({ ...config })
    ElMessage.success('保存成功')
  } finally {
    loading.value = false
  }
}

/* ===== 分类 CRUD ===== */
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const submitting = ref(false)
const form = reactive({
  id: null,
  name: '',
  code: '',
  description: '',
  questionFocus: '',
  sort: 50,
  enabled: true
})

const resetForm = () => {
  form.id = null
  form.name = ''
  form.code = ''
  form.description = ''
  form.questionFocus = ''
  form.sort = 50
  form.enabled = true
}

const onAdd = () => {
  resetForm()
  dialogTitle.value = '新增面试分类'
  dialogVisible.value = true
}

const onEdit = (row) => {
  Object.assign(form, row)
  dialogTitle.value = '编辑面试分类'
  dialogVisible.value = true
}

const onDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteInterviewCategory(row.id)
  ElMessage.success('删除成功')
  await loadCategories()
}

const onSubmit = async () => {
  if (!form.name.trim() || !form.code.trim()) {
    ElMessage.warning('名称和编码必填')
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await updateInterviewCategory(form.id, form)
      ElMessage.success('已更新')
    } else {
      await createInterviewCategory(form)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await loadCategories()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="interview-admin-layout">
    <div class="admin-subtabs" role="tablist">
      <button
        type="button"
        role="tab"
        class="admin-subtab"
        :class="{ active: activeSub === 'config' }"
        :aria-selected="activeSub === 'config'"
        @click="activeSub = 'config'"
      >
        面试全局配置
      </button>
      <button
        type="button"
        role="tab"
        class="admin-subtab"
        :class="{ active: activeSub === 'categories' }"
        :aria-selected="activeSub === 'categories'"
        @click="activeSub = 'categories'"
      >
        面试分类管理
      </button>
    </div>

    <template v-if="activeSub === 'config'">
      <div class="admin-panel-card card">
        <div class="admin-section-toolbar">
          <div>
            <h3>模拟面试配置</h3>
            <p>这些参数会被前端面试页与 AI 出题 / 评测过程读取，保存后立即对所有用户生效。</p>
          </div>
          <el-button type="primary" :loading="loading" @click="handleSaveConfig">保存配置</el-button>
        </div>
        <el-form label-position="top">
          <div class="config-grid">
            <el-form-item label="单场总时长（分钟）">
              <el-input-number v-model="config.interviewTotalMinutes" :min="3" :max="120" />
            </el-form-item>
            <el-form-item label="每场最大题量">
              <el-input-number v-model="config.interviewMaxQuestions" :min="1" :max="30" />
            </el-form-item>
            <el-form-item label="每人每日发起上限（0 = 无免费额度，需充值）">
              <el-input-number v-model="config.interviewDailyLimit" :min="0" :max="999" />
            </el-form-item>
          </div>
          <el-form-item label="面试官开场白">
            <el-input
              v-model="config.interviewOpening"
              type="textarea"
              :rows="2"
              placeholder="您好！欢迎参加本次模拟面试..."
            />
          </el-form-item>
          <el-form-item label="自我介绍提示语">
            <el-input
              v-model="config.interviewSelfIntroPrompt"
              type="textarea"
              :rows="2"
              placeholder="请先做一个 1 分钟的自我介绍..."
            />
          </el-form-item>
          <el-form-item label="AI 面试官系统 Prompt（人设）">
            <el-input
              v-model="config.interviewSystemPrompt"
              type="textarea"
              :rows="4"
              placeholder="你是一位资深技术面试官..."
            />
          </el-form-item>

          <el-divider content-position="left">沉浸式语音面试</el-divider>
          <el-form-item label="开启沉浸式语音面试">
            <el-switch v-model="config.interviewImmersiveEnabled" />
            <span class="immersive-hint">关闭后，用户端将隐藏「沉浸式语音面试」入口</span>
          </el-form-item>
          <div class="config-grid">
            <el-form-item label="单场消耗面试额度（次）">
              <el-input-number v-model="config.interviewImmersiveCost" :min="1" :max="20" :disabled="!config.interviewImmersiveEnabled" />
            </el-form-item>
            <el-form-item label="沉浸式总时长（分钟）">
              <el-input-number v-model="config.interviewImmersiveMinutes" :min="3" :max="120" :disabled="!config.interviewImmersiveEnabled" />
            </el-form-item>
          </div>

          <el-divider content-position="left">云端语音合成（TTS）</el-divider>
          <el-form-item label="启用云端音色（更自然，关闭则用浏览器本地语音）">
            <el-switch v-model="config.interviewTtsEnabled" />
          </el-form-item>
          <el-form-item label="云端语音 API Key">
            <el-input
              v-model="config.interviewTtsKey"
              :disabled="!config.interviewTtsEnabled"
              placeholder="hewoyi 语音合成密钥"
              show-password
              style="max-width: 420px"
            />
            <span class="immersive-hint">密钥保存在服务端，仅管理员可见，用户端接口不会返回</span>
          </el-form-item>
          <el-form-item label="高音质模型（tts-1-hd，更清晰但合成更慢）">
            <el-switch v-model="config.interviewTtsHd" :disabled="!config.interviewTtsEnabled" />
          </el-form-item>

          <el-divider content-position="left">讯飞云端语音识别（ASR）</el-divider>
          <el-form-item label="启用云端语音识别（用于微信等不支持浏览器原生识别的环境）">
            <el-switch v-model="config.interviewAsrEnabled" />
            <span class="immersive-hint">关闭后，微信等环境的沉浸式作答只能打字；桌面 Chrome/Edge 仍用浏览器原生识别</span>
          </el-form-item>
          <div class="config-grid">
            <el-form-item label="讯飞 APPID">
              <el-input
                v-model="config.interviewAsrAppId"
                :disabled="!config.interviewAsrEnabled"
                placeholder="讯飞控制台-我的应用"
                style="max-width: 420px"
              />
            </el-form-item>
            <el-form-item label="讯飞 APIKey">
              <el-input
                v-model="config.interviewAsrApiKey"
                :disabled="!config.interviewAsrEnabled"
                placeholder="语音听写（流式版）APIKey"
                show-password
                style="max-width: 420px"
              />
            </el-form-item>
          </div>
          <el-form-item label="讯飞 APISecret">
            <el-input
              v-model="config.interviewAsrApiSecret"
              :disabled="!config.interviewAsrEnabled"
              placeholder="语音听写（流式版）APISecret"
              show-password
              style="max-width: 420px"
            />
            <span class="immersive-hint">三项密钥保存在服务端，仅后端用于签名鉴权，用户端接口不会返回</span>
          </el-form-item>
        </el-form>
      </div>
    </template>

    <template v-else>
      <div class="admin-panel-card card">
        <div class="admin-section-toolbar">
          <div>
            <h3>面试分类管理（{{ categories.length }}）</h3>
            <p>分类用于指定 AI 出题方向。「提问方向」字段会作为 AI 出题的提示词，写得越具体出题越准。</p>
          </div>
          <el-button type="primary" @click="onAdd">新增分类</el-button>
        </div>
        <el-table :data="categories" stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="name" label="名称" width="160" />
          <el-table-column prop="code" label="编码" width="140" />
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
          <el-table-column prop="questionFocus" label="提问方向" show-overflow-tooltip />
          <el-table-column prop="sort" label="排序" width="70" />
          <el-table-column label="启用" width="80">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small" effect="plain">
                {{ row.enabled ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="onEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" link @click="onDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
        <el-form label-width="100px">
          <el-form-item label="分类名称" required>
            <el-input v-model="form.name" placeholder="如：Java 后端开发" />
          </el-form-item>
          <el-form-item label="编码" required>
            <el-input v-model="form.code" :disabled="!!form.id" placeholder="如：java_backend" />
            <div class="form-tip">仅小写英文 / 下划线，创建后不可修改</div>
          </el-form-item>
          <el-form-item label="分类描述">
            <el-input v-model="form.description" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="提问方向">
            <el-input
              v-model="form.questionFocus"
              type="textarea"
              :rows="2"
              placeholder="重点考察 Spring、并发、数据库...（AI 会按此方向出题）"
            />
          </el-form-item>
          <div class="form-row">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" :max="999" />
            </el-form-item>
            <el-form-item label="启用">
              <el-switch v-model="form.enabled" />
            </el-form-item>
          </div>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
        </template>
      </el-dialog>
    </template>
  </section>
</template>

<style scoped>
.interview-admin-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 20px;
}
.interview-admin-layout .admin-panel-card {
  margin-top: 0;
}
.config-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.immersive-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #999;
}
.form-row {
  display: flex;
  gap: 24px;
}
@media (max-width: 768px) {
  .config-grid { grid-template-columns: 1fr; }
  .form-row { flex-direction: column; gap: 0; }
}
</style>
