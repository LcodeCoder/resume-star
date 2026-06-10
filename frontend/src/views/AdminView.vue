<!--
  后台管理页面
  功能：统计概览 + 模板管理（创建、删除）+ AI 配置管理
  说明：创建模板时填写名称、分类、风格、主题色、版式，后端自动生成整套 A4 组件数据
-->
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createTemplate, deleteTemplate, getDashboard } from '../api/admin'
import { listTemplateCategories, listTemplates } from '../api/template'
import { listAiConfigs, saveAiConfig, deleteAiConfig, enableAiConfig } from '../api/aiConfig'
import { getSystemConfig, updateSystemConfig } from '../api/systemConfig'
import TemplatePreview from '../components/template-preview/TemplatePreview.vue'

const activeTab = ref('dashboard')
const dashboard = ref(null)
const categories = ref([])
const templates = ref([])
const creating = ref(false)
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

/** 创建模板表单数据 */
const form = reactive({
  name: '',
  categoryCode: 'tech',
  styleTag: '',
  accentColor: '#0a66c2',
  variant: 'classic',
  vipTemplate: false
})

/** 系统配置表单 */
const sysConfig = reactive({
  emailVerifyEnabled: false,
  emailUsername: '',
  emailPassword: '',
  singleIpEnabled: false,
  dailyExportLimit: 0,
  paymentEnabled: false,
  mockPaymentEnabled: true
})
const sysConfigLoading = ref(false)
const showEmailPassword = ref(false)

/** 版式选项：与后端 buildResumeComponents 支持的三种版式对应 */
const variantOptions = [
  { value: 'classic', label: '经典版（左对齐）' },
  { value: 'banner', label: '色带版（顶部色块）' },
  { value: 'minimal', label: '极简版（居中）' }
]

/** 刷新统计与模板列表 */
const refresh = async () => {
  dashboard.value = await getDashboard()
  templates.value = await listTemplates({ categoryCode: '' })
}

/** 刷新 AI 配置列表 */
const refreshAiConfigs = async () => {
  aiConfigs.value = await listAiConfigs()
}

/** 刷新系统配置 */
const refreshSysConfig = async () => {
  const config = await getSystemConfig()
  if (config) Object.assign(sysConfig, config)
}

onMounted(async () => {
  categories.value = await listTemplateCategories()
  await refresh()
  await refreshAiConfigs()
  await refreshSysConfig()
})

/**
 * 提交创建模板
 * 逻辑：校验名称后调用后台接口，成功后刷新列表并重置表单
 */
const handleCreate = async () => {
  if (!form.name.trim()) {
    ElMessage.warning('请填写模板名称')
    return
  }
  creating.value = true
  try {
    await createTemplate({ ...form })
    ElMessage.success('模板创建成功')
    form.name = ''
    form.styleTag = ''
    await refresh()
  } finally {
    creating.value = false
  }
}

/**
 * 删除模板（二次确认，防误删）
 */
const handleDelete = async (item) => {
  await ElMessageBox.confirm(`确定删除模板「${item.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteTemplate(item.id)
  ElMessage.success('已删除')
  await refresh()
}

/**
 * 打开 AI 配置对话框
 */
const openAiDialog = (config = null) => {
  if (config) {
    Object.assign(aiForm, config)
  } else {
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

/**
 * 保存 AI 配置
 */
const handleSaveAi = async () => {
  if (!aiForm.name || !aiForm.endpoint || !aiForm.apiKey || !aiForm.model) {
    ElMessage.warning('请填写完整信息')
    return
  }
  await saveAiConfig({ ...aiForm })
  ElMessage.success('保存成功')
  showAiDialog.value = false
  await refreshAiConfigs()
}

/**
 * 删除 AI 配置
 */
const handleDeleteAi = async (item) => {
  await ElMessageBox.confirm(`确定删除配置「${item.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteAiConfig(item.id)
  ElMessage.success('已删除')
  await refreshAiConfigs()
}

/**
 * 启用 AI 配置
 */
const handleEnableAi = async (item) => {
  await enableAiConfig(item.id)
  ElMessage.success(`已启用「${item.name}」`)
  await refreshAiConfigs()
}

/**
 * 保存系统配置
 */
const handleSaveSysConfig = async () => {
  sysConfigLoading.value = true
  try {
    await updateSystemConfig({ ...sysConfig })
    ElMessage.success('系统配置保存成功')
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || '未知错误'))
  } finally {
    sysConfigLoading.value = false
  }
}
</script>

<template>
  <section class="page-header card">
    <h2>后台管理</h2>
    <p>查看平台核心数据，管理简历模板与 AI 接口配置。</p>
    <div class="admin-tabs">
      <button class="admin-tab" :class="{ active: activeTab === 'dashboard' }" @click="activeTab = 'dashboard'">统计概览</button>
      <button class="admin-tab" :class="{ active: activeTab === 'templates' }" @click="activeTab = 'templates'">模板管理</button>
      <button class="admin-tab" :class="{ active: activeTab === 'ai' }" @click="activeTab = 'ai'">AI 配置</button>
      <button class="admin-tab" :class="{ active: activeTab === 'system' }" @click="activeTab = 'system'">系统配置</button>
    </div>
  </section>

  <!-- 统计概览 -->
  <section v-if="activeTab === 'dashboard' && dashboard" class="stat-grid">
    <div class="stat-card card"><span>用户数</span><strong>{{ dashboard.userCount }}</strong></div>
    <div class="stat-card card"><span>简历数</span><strong>{{ dashboard.resumeCount }}</strong></div>
    <div class="stat-card card"><span>模板数</span><strong>{{ dashboard.templateCount }}</strong></div>
    <div class="stat-card card"><span>今日 AI</span><strong>{{ dashboard.todayAiCalls }}</strong></div>
    <div class="stat-card card"><span>会员用户</span><strong>{{ dashboard.vipUserCount }}</strong></div>
    <div class="stat-card card"><span>套餐数</span><strong>{{ dashboard.packageCount }}</strong></div>
  </section>

  <!-- 模板管理：左侧创建表单，右侧模板列表 -->
  <section v-if="activeTab === 'templates'" class="admin-template-layout">
    <div class="card admin-form-card">
      <h3>创建模板</h3>
      <el-form label-position="top">
        <el-form-item label="模板名称" required>
          <el-input v-model="form.name" placeholder="如：经典蓝·后端工程师" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryCode" style="width: 100%">
            <el-option v-for="item in categories" :key="item.code" :value="item.code" :label="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="风格标签">
          <el-input v-model="form.styleTag" placeholder="如：清爽商务" />
        </el-form-item>
        <el-form-item label="版式">
          <el-select v-model="form.variant" style="width: 100%">
            <el-option v-for="item in variantOptions" :key="item.value" :value="item.value" :label="item.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="主题色">
          <el-color-picker v-model="form.accentColor" />
        </el-form-item>
        <el-form-item label="会员专属（仅标识，不拦截）">
          <el-switch v-model="form.vipTemplate" />
        </el-form-item>
        <el-button type="primary" class="full-button" :loading="creating" @click="handleCreate">创建模板</el-button>
      </el-form>
    </div>

    <div class="card admin-list-card">
      <h3>模板列表（{{ templates.length }}）</h3>
      <div class="admin-template-grid">
        <div v-for="item in templates" :key="item.id" class="admin-template-item">
          <TemplatePreview :components="item.components" />
          <div class="panel-template-name">
            <span>{{ item.name }}</span>
            <span v-if="item.vipTemplate" class="vip-badge">会员</span>
          </div>
          <el-button size="small" type="danger" plain class="full-button" @click="handleDelete(item)">删除</el-button>
        </div>
      </div>
    </div>
  </section>

  <!-- AI 配置管理 -->
  <section v-if="activeTab === 'ai'">
    <div class="card" style="padding: 24px">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px">
        <h3 style="margin: 0">AI 接口配置</h3>
        <el-button type="primary" @click="openAiDialog()">新增配置</el-button>
      </div>
      <el-table :data="aiConfigs" stripe style="width: 100%">
        <el-table-column prop="name" label="配置名称" width="150" />
        <el-table-column prop="endpoint" label="接口地址" min-width="200" show-overflow-tooltip />
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
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.enabled !== 1" size="small" type="success" @click="handleEnableAi(row)">启用</el-button>
            <el-button size="small" @click="openAiDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDeleteAi(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>

  <!-- 系统配置：邮箱验证、单IP登录、导出限制 -->
  <section v-if="activeTab === 'system'">
    <div class="card" style="padding: 28px">
      <h3 style="margin: 0 0 24px">系统配置</h3>

      <!-- 邮箱验证注册 -->
      <div class="sys-config-section">
        <div class="sys-config-header">
          <div>
            <h4>邮箱验证注册</h4>
            <p class="sys-config-desc">开启后用户注册时必须使用邮箱获取验证码绑定账户，需配置 QQ 邮箱 SMTP 服务</p>
          </div>
          <el-switch v-model="sysConfig.emailVerifyEnabled" active-text="开启" inactive-text="关闭" />
        </div>
        <el-form v-if="sysConfig.emailVerifyEnabled" label-position="top" class="sys-config-form">
          <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
            <template #title>
              <span>QQ 邮箱 SMTP 固定使用 smtp.qq.com:465 SSL，只需填写发送账号和授权码</span>
            </template>
          </el-alert>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="发送邮箱账号">
                <el-input v-model="sysConfig.emailUsername" placeholder="your-email@qq.com" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="邮箱授权码">
                <el-input v-model="sysConfig.emailPassword" :type="showEmailPassword ? 'text' : 'password'" placeholder="在 QQ 邮箱设置中获取授权码">
                  <template #suffix>
                    <el-icon style="cursor:pointer" @click="showEmailPassword = !showEmailPassword">
                      <svg v-if="showEmailPassword" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                      <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                    </el-icon>
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
            <template #title>
              <span>QQ 邮箱配置步骤：登录 QQ 邮箱 → 设置 → 账户 → 开启 SMTP 服务 → 生成授权码 → 将授权码填写到上方</span>
            </template>
          </el-alert>
        </el-form>
      </div>

      <!-- 单IP登录限制 -->
      <div class="sys-config-section">
        <div class="sys-config-header">
          <div>
            <h4>单 IP 登录限制</h4>
            <p class="sys-config-desc">开启后同一账号只能在一个设备上登录，新设备登录会自动踢出旧设备</p>
          </div>
          <el-switch v-model="sysConfig.singleIpEnabled" active-text="开启" inactive-text="关闭" />
        </div>
      </div>

      <!-- 每日导出限制 -->
      <div class="sys-config-section">
        <div class="sys-config-header">
          <div>
            <h4>每日导出数量限制</h4>
            <p class="sys-config-desc">限制用户每天可导出简历的次数，设为 0 表示不限制</p>
          </div>
        </div>
        <el-form label-position="top" class="sys-config-form">
          <el-form-item label="每日导出次数上限">
            <el-input-number v-model="sysConfig.dailyExportLimit" :min="0" :max="999" :step="1" />
            <span style="margin-left: 12px; color: #6e6e73; font-size: 13px">
              {{ sysConfig.dailyExportLimit === 0 ? '不限制' : `每人每天最多 ${sysConfig.dailyExportLimit} 次` }}
            </span>
          </el-form-item>
        </el-form>
      </div>

      <!-- 支付功能开关 -->
      <div class="sys-config-section">
        <div class="sys-config-header">
          <div>
            <h4>会员支付功能</h4>
            <p class="sys-config-desc">控制用户端是否展示购买入口；当前阶段仅支持模拟支付，不接入真实支付宝/微信网关</p>
          </div>
          <el-switch v-model="sysConfig.paymentEnabled" active-text="开启" inactive-text="关闭" />
        </div>
        <el-form v-if="sysConfig.paymentEnabled" label-position="top" class="sys-config-form">
          <el-form-item label="模拟支付">
            <el-switch v-model="sysConfig.mockPaymentEnabled" active-text="允许一键支付成功" inactive-text="仅创建待支付订单" />
            <span style="margin-left: 12px; color: #6e6e73; font-size: 13px">
              {{ sysConfig.mockPaymentEnabled ? '用户购买后会直接开通会员' : '用户只能创建订单，不能模拟支付成功' }}
            </span>
          </el-form-item>
        </el-form>
      </div>

      <el-button type="primary" :loading="sysConfigLoading" style="margin-top: 8px" @click="handleSaveSysConfig">保存配置</el-button>
    </div>
  </section>

  <!-- AI 配置对话框 -->
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
