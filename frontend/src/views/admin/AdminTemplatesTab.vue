<!--
  后台模板管理 Tab
  功能：创建、删除简历模板，模板 VIP 权限在 VIP 配置 Tab 中集中管理
-->
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { createTemplate, deleteTemplate } from '../../api/admin'
import { listTemplateCategories, listTemplates } from '../../api/template'
import TemplatePreview from '../../components/template-preview/TemplatePreview.vue'

const router = useRouter()
const categories = ref([])
const templates = ref([])
const creating = ref(false)

const form = reactive({
  name: '',
  categoryCode: 'tech',
  styleTag: '',
  accentColor: '#0a66c2',
  variant: 'classic',
  vipTemplate: false
})

const variantOptions = [
  { value: 'classic', label: '经典版（左对齐）' },
  { value: 'banner', label: '色带版（顶部色块）' },
  { value: 'minimal', label: '极简版（居中）' }
]

const refresh = async () => {
  templates.value = await listTemplates({ categoryCode: '' })
}

onMounted(async () => {
  categories.value = await listTemplateCategories()
  await refresh()
})

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

const handleDelete = async (item) => {
  await ElMessageBox.confirm(`确定删除模板「${item.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteTemplate(item.id)
  ElMessage.success('已删除')
  await refresh()
}

const handleEdit = (item) => {
  router.push({ path: '/editor', query: { templateId: item.id, adminMode: 'true' } })
}
</script>

<template>
  <section class="admin-template-layout">
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
        <el-form-item label="会员专属">
          <el-switch v-model="form.vipTemplate" active-text="创建为会员模板" />
        </el-form-item>
        <el-button type="primary" class="full-button" :loading="creating" @click="handleCreate">创建模板</el-button>
      </el-form>
    </div>

    <div class="card admin-list-card">
      <div class="admin-card-title">
        <h3>模板列表（{{ templates.length }}）</h3>
        <span>模板权限可在「VIP 配置」中批量调整。</span>
      </div>
      <div class="admin-template-grid">
        <div v-for="item in templates" :key="item.id" class="admin-template-item">
          <TemplatePreview :components="item.components" />
          <div class="panel-template-name">
            <span>{{ item.name }}</span>
            <span v-if="item.vipTemplate" class="vip-badge">会员</span>
          </div>
          <el-button size="small" type="primary" plain class="full-button" @click="handleEdit(item)">编辑内容</el-button>
          <el-button size="small" type="danger" plain class="full-button" @click="handleDelete(item)">删除</el-button>
        </div>
      </div>
    </div>
  </section>
</template>
