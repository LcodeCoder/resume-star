<!--
  后台 VIP 配置 Tab
  功能：配置组件分组和模板是否需要会员权限
-->
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getVipConfig, updateComponentVip, updateTemplateVip } from '../../api/admin'
import { listTemplates } from '../../api/template'
import { COMPONENT_TREE } from '../../data/componentLibrary'

const vipGroups = ref([])
const templates = ref([])
const savingGroup = ref('')
const savingTemplateId = ref(null)

const refresh = async () => {
  const [config, templateList] = await Promise.all([
    getVipConfig(),
    listTemplates({ categoryCode: '' })
  ])
  vipGroups.value = config?.vipComponentGroups || []
  templates.value = templateList || []
}

onMounted(refresh)

const isGroupVip = (key) => vipGroups.value.includes(key)

const toggleGroup = async (group, vipOnly) => {
  savingGroup.value = group.key
  try {
    await updateComponentVip({ groupKey: group.key, vipOnly })
    if (vipOnly && !vipGroups.value.includes(group.key)) vipGroups.value.push(group.key)
    if (!vipOnly) vipGroups.value = vipGroups.value.filter((item) => item !== group.key)
    ElMessage.success(`「${group.label}」权限已更新`)
  } finally {
    savingGroup.value = ''
  }
}

const toggleTemplate = async (template, vipTemplate) => {
  savingTemplateId.value = template.id
  try {
    await updateTemplateVip(template.id, { vipTemplate })
    template.vipTemplate = vipTemplate
    ElMessage.success(`「${template.name}」权限已更新`)
  } finally {
    savingTemplateId.value = null
  }
}
</script>

<template>
  <section class="admin-vip-layout">
    <div class="admin-panel-card card">
      <div class="admin-card-title">
        <h3>组件会员权限</h3>
        <span>按组件分组控制，避免逐个组件配置过细。</span>
      </div>
      <div class="vip-config-list">
        <div v-for="group in COMPONENT_TREE" :key="group.key" class="vip-config-row">
          <div>
            <strong>{{ group.label }}</strong>
            <p>{{ group.children.length }} 个组件｜key: {{ group.key }}</p>
          </div>
          <el-switch
            :model-value="isGroupVip(group.key)"
            :loading="savingGroup === group.key"
            active-text="会员专属"
            inactive-text="免费可用"
            @change="(value) => toggleGroup(group, value)"
          />
        </div>
      </div>
    </div>

    <div class="admin-panel-card card">
      <div class="admin-card-title">
        <h3>模板会员权限</h3>
        <span>控制模板库和编辑器侧栏中的模板是否需要会员。</span>
      </div>
      <el-table :data="templates" stripe style="width: 100%">
        <el-table-column prop="name" label="模板名称" min-width="180" />
        <el-table-column prop="industry" label="分类" width="120" />
        <el-table-column prop="styleTag" label="风格" width="130" />
        <el-table-column label="会员权限" width="180">
          <template #default="{ row }">
            <el-switch
              v-model="row.vipTemplate"
              :loading="savingTemplateId === row.id"
              active-text="会员"
              inactive-text="免费"
              @change="(value) => toggleTemplate(row, value)"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>
