<!--
  后台模板管理 Tab（合并版）
  功能：内嵌「模板分类」「创建/列表」「VIP 标记」三大子模块；模板分类的增删改查不再独立成 Tab
-->
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  createTemplate,
  deleteTemplate,
  listAdminTemplates,
  createTemplateCategory,
  updateTemplateCategory,
  deleteTemplateCategory
} from '../../api/admin'
import { listTemplateCategories } from '../../api/template'
import TemplatePreview from '../../components/template-preview/TemplatePreview.vue'

const router = useRouter()
const activeSub = ref('templates') // templates | categories
const categories = ref([])
const templates = ref([])
const creating = ref(false)

/* 模板列表后端分页 + 过滤 */
const tplPage = ref(1)
const tplSize = ref(12)
const tplTotal = ref(0)
const tplKeyword = ref('')
const tplCategory = ref('')
const tplLoading = ref(false)

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

/** 拉取当前页模板（后端分页 + 分类/关键字过滤） */
const loadTemplates = async () => {
  tplLoading.value = true
  try {
    const res = await listAdminTemplates({
      page: tplPage.value,
      size: tplSize.value,
      categoryCode: tplCategory.value || undefined,
      keyword: tplKeyword.value.trim() || undefined
    })
    templates.value = res?.records ?? []
    tplTotal.value = res?.total ?? 0
  } finally {
    tplLoading.value = false
  }
}

const refresh = async () => {
  categories.value = await listTemplateCategories()
  await loadTemplates()
}

/** 搜索/筛选：回到第 1 页 */
const handleTplSearch = () => {
  tplPage.value = 1
  loadTemplates()
}
const handleTplPageChange = (p) => {
  tplPage.value = p
  loadTemplates()
}

onMounted(refresh)

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
    tplPage.value = 1
    await loadTemplates()
  } finally {
    creating.value = false
  }
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm(`确定删除模板「${item.name}」吗？`, '删除确认', { type: 'warning' })
  } catch (e) {
    // 用户点取消/关闭，不应继续删除也不应抛错到上层错误边界
    return
  }
  await deleteTemplate(item.id)
  ElMessage.success('已删除')
  // 删除后当前页可能为空，回退一页
  if (templates.value.length === 1 && tplPage.value > 1) tplPage.value -= 1
  await loadTemplates()
}

const handleEdit = (item) => {
  router.push({ path: '/editor', query: { templateId: item.id, adminMode: 'true' } })
}

/* ===== 分类管理 ===== */
const catDialogVisible = ref(false)
const catDialogTitle = ref('新增分类')
const catLoading = ref(false)
const catForm = reactive({ id: null, name: '', code: '', sort: 0 })

const resetCatForm = () => {
  catForm.id = null
  catForm.name = ''
  catForm.code = ''
  catForm.sort = 0
}

const onAddCategory = () => {
  resetCatForm()
  catDialogTitle.value = '新增分类'
  catDialogVisible.value = true
}

const onEditCategory = (item) => {
  catForm.id = item.id
  catForm.name = item.name
  catForm.code = item.code
  catForm.sort = item.sort
  catDialogTitle.value = '编辑分类'
  catDialogVisible.value = true
}

const onDeleteCategory = async (item) => {
  try {
    await ElMessageBox.confirm(`确定删除分类「${item.name}」吗？`, '删除确认', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteTemplateCategory(item.id)
  ElMessage.success('删除成功')
  await refresh()
}

const onSubmitCategory = async () => {
  if (!catForm.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  if (!catForm.code.trim()) {
    ElMessage.warning('请输入分类编码')
    return
  }
  catLoading.value = true
  try {
    if (catForm.id) {
      await updateTemplateCategory(catForm.id, catForm)
      ElMessage.success('更新成功')
    } else {
      await createTemplateCategory(catForm)
      ElMessage.success('创建成功')
    }
    catDialogVisible.value = false
    await refresh()
  } finally {
    catLoading.value = false
  }
}
</script>

<template>
  <section class="admin-templates-page">
    <div class="admin-subtabs" role="tablist">
      <button
        type="button"
        role="tab"
        class="admin-subtab"
        :class="{ active: activeSub === 'templates' }"
        :aria-selected="activeSub === 'templates'"
        @click="activeSub = 'templates'"
      >
        模板创建与管理
      </button>
      <button
        type="button"
        role="tab"
        class="admin-subtab"
        :class="{ active: activeSub === 'categories' }"
        :aria-selected="activeSub === 'categories'"
        @click="activeSub = 'categories'"
      >
        模板分类维护
      </button>
    </div>

    <!-- 模板创建与管理：左侧 320 表单 + 右侧列表（沿用 admin-template-layout 全局 grid） -->
    <div v-if="activeSub === 'templates'" class="admin-template-layout">
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
          <h3>模板列表（{{ tplTotal }}）</h3>
          <span>模板权限可在「VIP 配置」中批量调整。</span>
        </div>
        <div class="admin-template-filter">
          <el-select v-model="tplCategory" placeholder="全部分类" clearable style="width: 160px" @change="handleTplSearch">
            <el-option v-for="item in categories" :key="item.code" :value="item.code" :label="item.name" />
          </el-select>
          <el-input
            v-model="tplKeyword"
            clearable
            placeholder="搜索模板名称 / 行业"
            style="width: 220px"
            @keyup.enter="handleTplSearch"
            @clear="handleTplSearch"
          >
            <template #append><el-button @click="handleTplSearch">搜索</el-button></template>
          </el-input>
        </div>
        <div class="admin-template-grid" v-loading="tplLoading">
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
        <div class="admin-template-pagination">
          <el-pagination
            background
            layout="total, prev, pager, next, jumper"
            :total="tplTotal"
            :current-page="tplPage"
            :page-size="tplSize"
            @current-change="handleTplPageChange"
          />
        </div>
      </div>
    </div>

    <!-- 模板分类维护：整宽面板卡片 -->
    <div v-else class="admin-panel-card card">
      <div class="admin-section-toolbar">
        <div>
          <h3>模板分类管理</h3>
          <p>维护简历模板的行业 / 风格分类，编码用于 URL 和 API，创建后不可修改。</p>
        </div>
        <el-button type="primary" @click="onAddCategory">新增分类</el-button>
      </div>
      <el-table :data="categories" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" min-width="160" />
        <el-table-column prop="code" label="分类编码" min-width="160" />
        <el-table-column prop="count" label="模板数" width="100" align="center" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="onEditCategory(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="onDeleteCategory(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="catDialogVisible" :title="catDialogTitle" width="500px">
      <el-form label-width="80px">
        <el-form-item label="分类名称" required>
          <el-input v-model="catForm.name" placeholder="如：互联网" />
        </el-form-item>
        <el-form-item label="分类编码" required>
          <el-input v-model="catForm.code" placeholder="如：tech" :disabled="!!catForm.id" />
          <div style="font-size: 12px; color: #999; margin-top: 4px">编码用于 URL 和 API，创建后不可修改</div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="catForm.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="catLoading" @click="onSubmitCategory">确定</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.admin-templates-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 20px;
}
.admin-templates-page .admin-panel-card {
  margin-top: 0;
}
.admin-templates-page .admin-template-layout {
  margin-top: 0;
}
.admin-template-filter {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.admin-template-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
