<!--
  后台 VIP 配置 Tab
  功能：配置组件会员权限（分组级 + 单组件级双粒度）和模板会员权限
  说明：分组开关一键把整组设为会员；展开分组后可对单个组件单独设会员，
        单组件标记优先级高于分组。用户端编辑器据此灰显锁定 + 拦截升级。
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getVipConfig,
  updateComponentVip,
  updateComponentKeyVip,
  updateTemplateVip
} from '../../api/admin'
import { listTemplates } from '../../api/template'
import { COMPONENT_TREE } from '../../data/componentLibrary'

const vipGroups = ref([])
const vipKeys = ref([])
const templates = ref([])
const savingGroup = ref('')
const savingKey = ref('')
const savingTemplateId = ref(null)
/** 当前展开查看单组件配置的分组 key */
const expandedGroup = ref('')

const refresh = async () => {
  const [config, templateList] = await Promise.all([
    getVipConfig(),
    listTemplates({ categoryCode: '' })
  ])
  vipGroups.value = config?.vipComponentGroups || []
  vipKeys.value = config?.vipComponentKeys || []
  templates.value = templateList || []
}

onMounted(refresh)

/** 单组件唯一 key：分组 + 名称，与用户端 componentKeyOf 保持一致 */
const componentKeyOf = (groupKey, label) => `${groupKey}:${label}`

const isGroupVip = (key) => vipGroups.value.includes(key)
const isKeyVip = (groupKey, label) => vipKeys.value.includes(componentKeyOf(groupKey, label))

/** 某分组里被单独设为会员的组件数 */
const groupVipCount = (group) =>
  group.children.filter((c) => isKeyVip(group.key, c.label)).length

const toggleExpand = (key) => {
  expandedGroup.value = expandedGroup.value === key ? '' : key
}

const toggleGroup = async (group, vipOnly) => {
  savingGroup.value = group.key
  try {
    await updateComponentVip({ groupKey: group.key, vipOnly })
    if (vipOnly && !vipGroups.value.includes(group.key)) vipGroups.value.push(group.key)
    if (!vipOnly) vipGroups.value = vipGroups.value.filter((item) => item !== group.key)
    ElMessage.success(`「${group.label}」整组权限已更新`)
  } finally {
    savingGroup.value = ''
  }
}

const toggleComponent = async (group, child, vipOnly) => {
  const key = componentKeyOf(group.key, child.label)
  savingKey.value = key
  try {
    await updateComponentKeyVip({ componentKey: key, vipOnly })
    if (vipOnly && !vipKeys.value.includes(key)) vipKeys.value.push(key)
    if (!vipOnly) vipKeys.value = vipKeys.value.filter((item) => item !== key)
    ElMessage.success(`「${child.label}」权限已更新`)
  } finally {
    savingKey.value = ''
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
        <span>分组开关一键设整组；展开可对单个组件单独设会员（单组件优先级更高）。</span>
      </div>
      <div class="vip-config-list">
        <div v-for="group in COMPONENT_TREE" :key="group.key" class="vip-config-block">
          <div class="vip-config-row">
            <div class="vip-config-info">
              <button class="vip-expand-btn" :class="{ open: expandedGroup === group.key }" @click="toggleExpand(group.key)">▸</button>
              <div>
                <strong>{{ group.label }}</strong>
                <p>
                  {{ group.children.length }} 个组件｜key: {{ group.key }}
                  <span v-if="groupVipCount(group)" class="vip-count-tag">单设会员 {{ groupVipCount(group) }}</span>
                </p>
              </div>
            </div>
            <el-switch
              :model-value="isGroupVip(group.key)"
              :loading="savingGroup === group.key"
              active-text="整组会员"
              inactive-text="免费"
              @change="(value) => toggleGroup(group, value)"
            />
          </div>

          <!-- 展开：单组件级会员开关 -->
          <div v-show="expandedGroup === group.key" class="vip-child-grid">
            <div v-for="child in group.children" :key="child.label" class="vip-child-row">
              <span class="vip-child-label" :class="{ 'is-vip': isGroupVip(group.key) || isKeyVip(group.key, child.label) }">
                {{ child.label }}
              </span>
              <el-switch
                size="small"
                :disabled="isGroupVip(group.key)"
                :model-value="isGroupVip(group.key) || isKeyVip(group.key, child.label)"
                :loading="savingKey === componentKeyOf(group.key, child.label)"
                @change="(value) => toggleComponent(group, child, value)"
              />
            </div>
            <p v-if="isGroupVip(group.key)" class="vip-child-hint">整组已设为会员，关闭整组开关后可单独配置。</p>
          </div>
        </div>
      </div>
    </div>

    <div class="admin-panel-card card">
      <div class="admin-card-title">
        <h3>模板会员权限</h3>
        <span>控制模板库和编辑器侧栏中的模板是否需要会员。</span>
      </div>
      <el-table :data="templates" stripe>
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

<style scoped>
.vip-config-block {
  border-bottom: 1px solid #f0f0f2;
}
.vip-config-block:last-child {
  border-bottom: none;
}
.vip-config-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.vip-expand-btn {
  border: none;
  background: #f5f5f7;
  border-radius: 6px;
  width: 24px;
  height: 24px;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: transform 0.15s ease;
  flex-shrink: 0;
}
.vip-expand-btn.open {
  transform: rotate(90deg);
  color: #5b5bd6;
}
.vip-count-tag {
  display: inline-block;
  margin-left: 6px;
  padding: 0 6px;
  border-radius: 4px;
  background: #fff4e6;
  color: #b9770a;
  font-size: 11px;
}
.vip-child-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 4px 16px;
  padding: 8px 0 14px 34px;
}
.vip-child-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 10px;
  border-radius: 6px;
  background: #fafafa;
}
.vip-child-label {
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.vip-child-label.is-vip {
  color: #5b5bd6;
  font-weight: 600;
}
.vip-child-hint {
  grid-column: 1 / -1;
  font-size: 12px;
  color: #9e9ea4;
  margin: 2px 0 0;
}

/* ===== 暗色模式：折叠按钮 / 计数标签 / 子项行 ===== */
:root[data-theme^='night'] .vip-expand-btn {
  background: var(--color-elevated);
}

:root[data-theme^='night'] .vip-count-tag {
  background: rgba(245, 158, 11, 0.18);
  color: #f0c674;
}

:root[data-theme^='night'] .vip-child-row {
  background: var(--color-elevated);
}

:root[data-theme^='night'] .vip-child-label.is-vip,
:root[data-theme^='night'] .vip-expand-btn.open {
  color: #aab0ff;
}
</style>
