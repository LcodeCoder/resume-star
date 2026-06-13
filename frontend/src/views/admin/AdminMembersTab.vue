<!--
  后台会员管理 Tab
  功能：分「会员体系 / 次数充值」两个分段——
        会员体系：维护会员套餐（等级、价格、有效期、每日额度）与会员兑换码（邀请码）；
        次数充值：维护额度套餐（一次性次数包）与额度兑换码
-->
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listMemberPackages, saveMemberPackage, deleteMemberPackage,
  listRedeemCodes, generateRedeemCodes, deleteRedeemCode,
  listQuotaPackages, saveQuotaPackage, deleteQuotaPackage,
  listQuotaCodes, generateQuotaCodes, deleteQuotaCode
} from '../../api/admin'

const packages = ref([])
const codes = ref([])
const quotaPackages = ref([])
const quotaCodes = ref([])

/* 页内分段：会员体系 / 次数充值 */
const activeSection = ref('member')
const sections = [
  { value: 'member', label: '会员体系' },
  { value: 'quota', label: '次数充值' }
]

/* 各表格独立加载态，避免一处刷新全页转圈 */
const loading = reactive({ pkgs: false, codes: false, quotaPkgs: false, quotaCodes: false })

/* 兑换码列表客户端分页 */
const CODE_PAGE_SIZE = 10
const codePage = ref(1)
const quotaCodePage = ref(1)
const pagedCodes = computed(() =>
  codes.value.slice((codePage.value - 1) * CODE_PAGE_SIZE, codePage.value * CODE_PAGE_SIZE)
)
const pagedQuotaCodes = computed(() =>
  quotaCodes.value.slice((quotaCodePage.value - 1) * CODE_PAGE_SIZE, quotaCodePage.value * CODE_PAGE_SIZE)
)

/* ===== 会员套餐 ===== */
const pkgDialogVisible = ref(false)
const pkgForm = reactive({
  id: null, name: '', price: 19.9, validDays: 30,
  dailyAiQuota: 20, dailyExportQuota: 10, dailyInterviewQuota: 1, benefitsText: '', recommended: false
})

const refreshPackages = async () => {
  loading.pkgs = true
  try {
    packages.value = await listMemberPackages()
  } finally {
    loading.pkgs = false
  }
  if (!codeForm.packageId && packages.value.length) {
    codeForm.packageId = packages.value[0].id
  }
}

const openCreatePkg = () => {
  Object.assign(pkgForm, {
    id: null, name: '', price: 19.9, validDays: 30,
    dailyAiQuota: 20, dailyExportQuota: 10, dailyInterviewQuota: 1, benefitsText: '', recommended: false
  })
  pkgDialogVisible.value = true
}

const openEditPkg = (row) => {
  Object.assign(pkgForm, {
    id: row.id, name: row.name, price: Number(row.price),
    validDays: row.validDays, dailyAiQuota: row.dailyAiQuota, dailyExportQuota: row.dailyExportQuota,
    dailyInterviewQuota: row.dailyInterviewQuota ?? 0,
    benefitsText: (row.benefits || []).join('\n'), recommended: !!row.recommended
  })
  pkgDialogVisible.value = true
}

const savePkg = async () => {
  if (!pkgForm.name.trim()) {
    ElMessage.warning('请填写套餐名称')
    return
  }
  const payload = {
    id: pkgForm.id,
    name: pkgForm.name.trim(),
    price: pkgForm.price,
    validDays: pkgForm.validDays,
    dailyAiQuota: pkgForm.dailyAiQuota,
    dailyExportQuota: pkgForm.dailyExportQuota,
    dailyInterviewQuota: pkgForm.dailyInterviewQuota,
    benefits: pkgForm.benefitsText.split('\n').map((s) => s.trim()).filter(Boolean),
    recommended: pkgForm.recommended
  }
  await saveMemberPackage(payload)
  ElMessage.success(pkgForm.id ? '套餐已更新' : '套餐已新增')
  pkgDialogVisible.value = false
  await refreshPackages()
}

const removePkg = async (row) => {
  await ElMessageBox.confirm(`确定删除套餐「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteMemberPackage(row.id)
  ElMessage.success('套餐已删除')
  await refreshPackages()
}

/* ===== 兑换码 ===== */
const codeForm = reactive({ packageId: null, count: 5 })

const refreshCodes = async () => {
  loading.codes = true
  try {
    codes.value = await listRedeemCodes()
  } finally {
    loading.codes = false
  }
}

const handleGenerate = async () => {
  if (!codeForm.packageId) {
    ElMessage.warning('请先选择要绑定的会员套餐')
    return
  }
  const created = await generateRedeemCodes({ packageId: codeForm.packageId, count: codeForm.count })
  ElMessage.success(`已生成 ${created.length} 个兑换码`)
  codePage.value = 1
  await refreshCodes()
}

const copyCode = async (code) => {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('兑换码已复制')
  } catch (e) {
    ElMessage.warning('复制失败，请手动复制')
  }
}

const removeCode = async (row) => {
  await ElMessageBox.confirm(`确定删除兑换码「${row.code}」吗？`, '删除确认', { type: 'warning' })
  await deleteRedeemCode(row.id)
  ElMessage.success('兑换码已删除')
  await refreshCodes()
}

const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '-')

/* ===== 兑换码批量导出 / 批量复制 ===== */

/** 生成并下载 CSV 文件（带 BOM，Excel 打开中文不乱码） */
const downloadCsv = (filename, header, rows) => {
  const esc = (v) => {
    const s = v == null ? '' : String(v)
    return /[",\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s
  }
  const content = '\ufeff' + [header, ...rows].map((r) => r.map(esc).join(',')).join('\n')
  const url = URL.createObjectURL(new Blob([content], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

const today = () => new Date().toISOString().slice(0, 10)

/** 导出未使用的会员兑换码 CSV（发卡给小店用） */
const exportCodesCsv = () => {
  const unused = codes.value.filter((c) => !c.used)
  if (!unused.length) {
    ElMessage.warning('没有未使用的兑换码可导出')
    return
  }
  downloadCsv(`会员兑换码-未使用-${today()}.csv`, ['兑换码', '套餐', '金额(元)', '有效天数'],
    unused.map((c) => [c.code, c.packageName || c.levelName, c.price ?? '', c.validDays ?? '']))
  ElMessage.success(`已导出 ${unused.length} 个未使用兑换码`)
}

/** 复制全部未使用的会员兑换码（每行一个） */
const copyAllCodes = async () => {
  const unused = codes.value.filter((c) => !c.used)
  if (!unused.length) {
    ElMessage.warning('没有未使用的兑换码')
    return
  }
  try {
    await navigator.clipboard.writeText(unused.map((c) => c.code).join('\n'))
    ElMessage.success(`已复制 ${unused.length} 个未使用兑换码`)
  } catch (e) {
    ElMessage.warning('复制失败，请改用导出 CSV')
  }
}

/** 导出未使用的额度兑换码 CSV */
const exportQuotaCodesCsv = () => {
  const unused = quotaCodes.value.filter((c) => !c.used)
  if (!unused.length) {
    ElMessage.warning('没有未使用的额度兑换码可导出')
    return
  }
  downloadCsv(`额度兑换码-未使用-${today()}.csv`, ['兑换码', '套餐', 'AI 次数', '导出次数', '面值(元)'],
    unused.map((c) => [c.code, c.packageName, c.aiCount ?? '', c.exportCount ?? '', c.price ?? '']))
  ElMessage.success(`已导出 ${unused.length} 个未使用额度兑换码`)
}

/** 复制全部未使用的额度兑换码（每行一个） */
const copyAllQuotaCodes = async () => {
  const unused = quotaCodes.value.filter((c) => !c.used)
  if (!unused.length) {
    ElMessage.warning('没有未使用的额度兑换码')
    return
  }
  try {
    await navigator.clipboard.writeText(unused.map((c) => c.code).join('\n'))
    ElMessage.success(`已复制 ${unused.length} 个未使用额度兑换码`)
  } catch (e) {
    ElMessage.warning('复制失败，请改用导出 CSV')
  }
}

/* ===== 额度套餐（次数包） ===== */
const quotaPkgDialogVisible = ref(false)
const quotaPkgForm = reactive({
  id: null, name: '', aiCount: 10, exportCount: 10, interviewCount: 0, price: 9.9, benefitsText: '', recommended: false
})

const refreshQuotaPackages = async () => {
  loading.quotaPkgs = true
  try {
    quotaPackages.value = await listQuotaPackages()
  } finally {
    loading.quotaPkgs = false
  }
  if (!quotaCodeForm.packageId && quotaPackages.value.length) {
    quotaCodeForm.packageId = quotaPackages.value[0].id
  }
}

const openCreateQuotaPkg = () => {
  Object.assign(quotaPkgForm, {
    id: null, name: '', aiCount: 10, exportCount: 10, interviewCount: 0, price: 9.9, benefitsText: '', recommended: false
  })
  quotaPkgDialogVisible.value = true
}

const openEditQuotaPkg = (row) => {
  Object.assign(quotaPkgForm, {
    id: row.id, name: row.name, aiCount: row.aiCount, exportCount: row.exportCount, interviewCount: row.interviewCount ?? 0,
    price: Number(row.price), benefitsText: (row.benefits || []).join('\n'), recommended: !!row.recommended
  })
  quotaPkgDialogVisible.value = true
}

const saveQuotaPkg = async () => {
  if (!quotaPkgForm.name.trim()) {
    ElMessage.warning('请填写套餐名称')
    return
  }
  const payload = {
    id: quotaPkgForm.id,
    name: quotaPkgForm.name.trim(),
    aiCount: quotaPkgForm.aiCount,
    exportCount: quotaPkgForm.exportCount,
    interviewCount: quotaPkgForm.interviewCount,
    price: quotaPkgForm.price,
    benefits: quotaPkgForm.benefitsText.split('\n').map((s) => s.trim()).filter(Boolean),
    recommended: quotaPkgForm.recommended
  }
  await saveQuotaPackage(payload)
  ElMessage.success(quotaPkgForm.id ? '额度套餐已更新' : '额度套餐已新增')
  quotaPkgDialogVisible.value = false
  await refreshQuotaPackages()
}

const removeQuotaPkg = async (row) => {
  await ElMessageBox.confirm(`确定删除额度套餐「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteQuotaPackage(row.id)
  ElMessage.success('额度套餐已删除')
  await refreshQuotaPackages()
}

/* ===== 额度兑换码 ===== */
const quotaCodeForm = reactive({ packageId: null, count: 5 })

const refreshQuotaCodes = async () => {
  loading.quotaCodes = true
  try {
    quotaCodes.value = await listQuotaCodes()
  } finally {
    loading.quotaCodes = false
  }
}

const handleGenerateQuota = async () => {
  if (!quotaCodeForm.packageId) {
    ElMessage.warning('请先选择要绑定的额度套餐')
    return
  }
  const created = await generateQuotaCodes({ packageId: quotaCodeForm.packageId, count: quotaCodeForm.count })
  ElMessage.success(`已生成 ${created.length} 个额度兑换码`)
  quotaCodePage.value = 1
  await refreshQuotaCodes()
}

const removeQuotaCode = async (row) => {
  await ElMessageBox.confirm(`确定删除额度兑换码「${row.code}」吗？`, '删除确认', { type: 'warning' })
  await deleteQuotaCode(row.id)
  ElMessage.success('额度兑换码已删除')
  await refreshQuotaCodes()
}

onMounted(async () => {
  await Promise.all([refreshPackages(), refreshCodes(), refreshQuotaPackages(), refreshQuotaCodes()])
})
</script>

<template>
  <section class="admin-member-layout">
    <!-- 页内分段：会员体系（按周期） / 次数充值（一次性次数包） -->
    <div class="admin-subtabs" role="tablist">
      <button
        v-for="item in sections"
        :key="item.value"
        type="button"
        role="tab"
        class="admin-subtab"
        :class="{ active: activeSection === item.value }"
        :aria-selected="activeSection === item.value"
        @click="activeSection = item.value"
      >
        {{ item.label }}
      </button>
    </div>

    <!-- ===== 分段一：会员体系 ===== -->
    <template v-if="activeSection === 'member'">
    <!-- 会员套餐管理 -->
    <div class="admin-panel-card card">
      <div class="admin-section-toolbar">
        <div>
          <h3>会员套餐管理</h3>
          <p>维护会员等级、价格、有效期与每日 AI / 导出额度，支持新增和删除套餐种类。</p>
        </div>
        <el-button type="primary" @click="openCreatePkg">新增套餐</el-button>
      </div>

      <el-table v-loading="loading.pkgs" :data="packages" stripe>
        <template #empty>
          <el-empty description="还没有会员套餐，点击右上角「新增套餐」创建" />
        </template>
        <el-table-column prop="name" label="套餐名称" min-width="120">
          <template #default="{ row }">
            {{ row.name }}
            <el-tag v-if="row.recommended" type="success" size="small" effect="plain">推荐</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="90">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="validDays" label="有效天数" width="100" />
        <el-table-column label="每日 AI 次数" width="110">
          <template #default="{ row }">{{ row.dailyAiQuota }}</template>
        </el-table-column>
        <el-table-column label="每日导出次数" width="120">
          <template #default="{ row }">{{ row.dailyExportQuota }}</template>
        </el-table-column>
        <el-table-column label="每日面试次数" width="120">
          <template #default="{ row }">{{ row.dailyInterviewQuota ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="权益" min-width="180">
          <template #default="{ row }">
            <span class="member-benefits">{{ (row.benefits || []).join('，') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditPkg(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="removePkg(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 兑换码管理 -->
    <div class="admin-panel-card card">
      <div class="admin-section-toolbar">
        <div>
          <h3>会员兑换码（邀请码）</h3>
          <p>生成绑定会员等级与有效期的兑换码，用户在会员中心输入即可开通对应会员。</p>
        </div>
        <div class="toolbar-actions">
          <el-button plain @click="copyAllCodes">复制未用</el-button>
          <el-button plain @click="exportCodesCsv">导出未用 CSV</el-button>
        </div>
      </div>

      <div class="redeem-gen-row">
        <span class="redeem-gen-label">绑定套餐</span>
        <el-select v-model="codeForm.packageId" placeholder="选择套餐" class="redeem-gen-select">
          <el-option
            v-for="pkg in packages"
            :key="pkg.id"
            :label="`${pkg.name}（¥${pkg.price} / ${pkg.validDays}天）`"
            :value="pkg.id"
          />
        </el-select>
        <span class="redeem-gen-label">数量</span>
        <el-input-number v-model="codeForm.count" :min="1" :max="50" />
        <el-button type="primary" @click="handleGenerate">生成兑换码</el-button>
      </div>

      <el-table v-loading="loading.codes" :data="pagedCodes" stripe>
        <template #empty>
          <el-empty description="暂无兑换码，选择套餐后点击「生成兑换码」" />
        </template>
        <el-table-column prop="code" label="兑换码" min-width="180">
          <template #default="{ row }"><span class="redeem-code-text">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column label="套餐" width="130">
          <template #default="{ row }">{{ row.packageName || row.levelName }}</template>
        </el-table-column>
        <el-table-column label="金额" width="90">
          <template #default="{ row }">¥{{ row.price != null ? row.price : '-' }}</template>
        </el-table-column>
        <el-table-column prop="validDays" label="有效天数" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.used ? 'info' : 'success'" size="small"><i class="tag-dot" />{{ row.used ? '已使用' : '未使用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="使用时间" width="160">
          <template #default="{ row }">{{ row.used ? fmtTime(row.usedTime) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="copyCode(row.code)">复制</el-button>
            <el-button size="small" type="danger" plain @click="removeCode(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="codes.length > CODE_PAGE_SIZE"
        v-model:current-page="codePage"
        :page-size="CODE_PAGE_SIZE"
        :total="codes.length"
        layout="total, prev, pager, next"
        background
      />
    </div>
    </template>

    <!-- ===== 分段二：次数充值 ===== -->
    <template v-if="activeSection === 'quota'">
    <!-- 额度套餐管理（次数包） -->
    <div class="admin-panel-card card">
      <div class="admin-section-toolbar">
        <div>
          <h3>额度套餐管理</h3>
          <p>配置一次性「次数包」（充值卡）：兑换后把 AI / 导出次数累加到用户余额，用完为止、不每日恢复。适合每日额度设为 0 时按次发放。</p>
        </div>
        <el-button type="primary" @click="openCreateQuotaPkg">新增额度套餐</el-button>
      </div>

      <el-table v-loading="loading.quotaPkgs" :data="quotaPackages" stripe>
        <template #empty>
          <el-empty description="还没有额度套餐，点击右上角「新增额度套餐」创建" />
        </template>
        <el-table-column prop="name" label="套餐名称" min-width="120">
          <template #default="{ row }">
            {{ row.name }}
            <el-tag v-if="row.recommended" type="success" size="small" effect="plain">推荐</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="AI 次数" width="100">
          <template #default="{ row }">+{{ row.aiCount }}</template>
        </el-table-column>
        <el-table-column label="导出次数" width="100">
          <template #default="{ row }">+{{ row.exportCount }}</template>
        </el-table-column>
        <el-table-column label="面试次数" width="100">
          <template #default="{ row }">+{{ row.interviewCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="面值" width="90">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="权益" min-width="180">
          <template #default="{ row }">
            <span class="member-benefits">{{ (row.benefits || []).join('，') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditQuotaPkg(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="removeQuotaPkg(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 额度兑换码管理 -->
    <div class="admin-panel-card card">
      <div class="admin-section-toolbar">
        <div>
          <h3>额度兑换码</h3>
          <p>按额度套餐生成次数兑换码，用户在会员中心输入即可把对应 AI / 导出次数充入余额。</p>
        </div>
        <div class="toolbar-actions">
          <el-button plain @click="copyAllQuotaCodes">复制未用</el-button>
          <el-button plain @click="exportQuotaCodesCsv">导出未用 CSV</el-button>
        </div>
      </div>

      <div class="redeem-gen-row">
        <span class="redeem-gen-label">绑定套餐</span>
        <el-select v-model="quotaCodeForm.packageId" placeholder="选择额度套餐" class="redeem-gen-select wide">
          <el-option
            v-for="pkg in quotaPackages"
            :key="pkg.id"
            :label="`${pkg.name}（AI+${pkg.aiCount} / 导出+${pkg.exportCount}）`"
            :value="pkg.id"
          />
        </el-select>
        <span class="redeem-gen-label">数量</span>
        <el-input-number v-model="quotaCodeForm.count" :min="1" :max="50" />
        <el-button type="primary" @click="handleGenerateQuota">生成额度兑换码</el-button>
      </div>

      <el-table v-loading="loading.quotaCodes" :data="pagedQuotaCodes" stripe>
        <template #empty>
          <el-empty description="暂无额度兑换码，选择套餐后点击「生成额度兑换码」" />
        </template>
        <el-table-column prop="code" label="兑换码" min-width="180">
          <template #default="{ row }"><span class="redeem-code-text">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column label="套餐" width="120">
          <template #default="{ row }">{{ row.packageName }}</template>
        </el-table-column>
        <el-table-column label="AI 次数" width="90">
          <template #default="{ row }">+{{ row.aiCount }}</template>
        </el-table-column>
        <el-table-column label="导出次数" width="90">
          <template #default="{ row }">+{{ row.exportCount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.used ? 'info' : 'success'" size="small"><i class="tag-dot" />{{ row.used ? '已使用' : '未使用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="使用时间" width="150">
          <template #default="{ row }">{{ row.used ? fmtTime(row.usedTime) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="copyCode(row.code)">复制</el-button>
            <el-button size="small" type="danger" plain @click="removeQuotaCode(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="quotaCodes.length > CODE_PAGE_SIZE"
        v-model:current-page="quotaCodePage"
        :page-size="CODE_PAGE_SIZE"
        :total="quotaCodes.length"
        layout="total, prev, pager, next"
        background
      />
    </div>
    </template>
  </section>

  <!-- 套餐新增 / 编辑弹窗 -->
  <el-dialog v-model="pkgDialogVisible" :title="pkgForm.id ? '编辑会员套餐' : '新增会员套餐'" width="460px">
    <el-form label-position="top">
      <el-form-item label="套餐名称">
        <el-input v-model="pkgForm.name" placeholder="如：专业会员" maxlength="20" />
      </el-form-item>
      <div class="pkg-form-row">
        <el-form-item label="价格（元）">
          <el-input-number v-model="pkgForm.price" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="有效天数">
          <el-input-number v-model="pkgForm.validDays" :min="1" :max="3650" />
        </el-form-item>
      </div>
      <div class="pkg-form-row">
        <el-form-item label="每日 AI 次数">
          <el-input-number v-model="pkgForm.dailyAiQuota" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="每日导出次数">
          <el-input-number v-model="pkgForm.dailyExportQuota" :min="0" :max="9999" />
        </el-form-item>
      </div>
      <el-form-item label="每日模拟面试次数">
        <el-input-number v-model="pkgForm.dailyInterviewQuota" :min="0" :max="99" />
      </el-form-item>
      <el-form-item label="权益说明（每行一条）">
        <el-input v-model="pkgForm.benefitsText" type="textarea" :rows="3" placeholder="每日 AI 100 次&#10;每日导出 50 次" />
      </el-form-item>
      <el-form-item>
        <el-checkbox v-model="pkgForm.recommended">标记为推荐套餐</el-checkbox>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pkgDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="savePkg">保存</el-button>
    </template>
  </el-dialog>

  <!-- 额度套餐新增 / 编辑弹窗 -->
  <el-dialog v-model="quotaPkgDialogVisible" :title="quotaPkgForm.id ? '编辑额度套餐' : '新增额度套餐'" width="460px">
    <el-form label-position="top">
      <el-form-item label="套餐名称">
        <el-input v-model="quotaPkgForm.name" placeholder="如：导出次数包" maxlength="20" />
      </el-form-item>
      <div class="pkg-form-row">
        <el-form-item label="赠送 AI 次数">
          <el-input-number v-model="quotaPkgForm.aiCount" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="赠送导出次数">
          <el-input-number v-model="quotaPkgForm.exportCount" :min="0" :max="9999" />
        </el-form-item>
      </div>
      <div class="pkg-form-row">
        <el-form-item label="赠送模拟面试次数">
          <el-input-number v-model="quotaPkgForm.interviewCount" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="面值（元）">
          <el-input-number v-model="quotaPkgForm.price" :min="0" :precision="2" :step="5" />
        </el-form-item>
      </div>
      <el-form-item label="权益说明（每行一条）">
        <el-input v-model="quotaPkgForm.benefitsText" type="textarea" :rows="3" placeholder="导出次数 +10&#10;永久有效，用完为止" />
      </el-form-item>
      <el-form-item>
        <el-checkbox v-model="quotaPkgForm.recommended">标记为推荐套餐</el-checkbox>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="quotaPkgDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveQuotaPkg">保存</el-button>
    </template>
  </el-dialog>
</template>
