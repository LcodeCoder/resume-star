<!--
  会员中心页面
  功能：展示会员套餐、支付开关状态、模拟支付购买入口和订单记录
-->
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createPaymentOrder, listMemberPackages, listPaymentOrders, mockPayOrder } from '../api/member'
import { getUserSystemConfig } from '../api/user'
import { useUserStore } from '../store/user'
import MemberUpgradeDialog from '../components/member-tip/MemberUpgradeDialog.vue'

const userStore = useUserStore()
const packages = ref([])
const orders = ref([])
const visible = ref(false)
const loadingPackageId = ref(null)
const systemConfig = ref({ paymentEnabled: false, mockPaymentEnabled: true })

const refreshOrders = async () => {
  orders.value = await listPaymentOrders({ userId: userStore.profile?.id || 1 })
}

const refresh = async () => {
  await userStore.loadProfile()
  const [packageList, config] = await Promise.all([
    listMemberPackages(),
    getUserSystemConfig()
  ])
  packages.value = packageList
  systemConfig.value = config || systemConfig.value
  await refreshOrders()
}

onMounted(refresh)

const handleBuy = async (item) => {
  if (!systemConfig.value.paymentEnabled) {
    ElMessage.warning('支付功能暂未开启，请联系管理员')
    return
  }
  loadingPackageId.value = item.id
  try {
    const order = await createPaymentOrder({ userId: userStore.profile?.id || 1, packageId: item.id, payChannel: 'MOCK' })
    if (systemConfig.value.mockPaymentEnabled) {
      await mockPayOrder(order.orderNo, { userId: userStore.profile?.id || 1 })
      ElMessage.success(`模拟支付成功，已开通${item.name}`)
      await userStore.loadProfile()
    } else {
      ElMessage.success('订单已创建，请等待支付功能开放')
    }
    await refreshOrders()
  } finally {
    loadingPackageId.value = null
  }
}
</script>

<template>
  <section class="page-header card">
    <h2>会员中心</h2>
    <p>选择适合你的会员套餐，解锁更多模板与 AI 优化额度。</p>
    <div class="payment-status-row">
      <el-tag :type="systemConfig.paymentEnabled ? 'success' : 'info'">
        {{ systemConfig.paymentEnabled ? '支付已开启' : '支付未开启' }}
      </el-tag>
      <el-tag :type="systemConfig.mockPaymentEnabled ? 'warning' : 'info'">
        {{ systemConfig.mockPaymentEnabled ? '模拟支付可用' : '模拟支付关闭' }}
      </el-tag>
    </div>
    <el-alert
      v-if="!systemConfig.paymentEnabled"
      type="info"
      :closable="false"
      show-icon
      title="管理员暂未开启支付功能，当前仅展示会员套餐。"
      style="margin-top: 16px"
    />
    <el-button type="primary" style="margin-top: 16px" @click="visible = true">查看套餐对比</el-button>
  </section>

  <section class="template-grid">
    <article v-for="item in packages" :key="item.id" class="template-card card">
      <h3>{{ item.name }}</h3>
      <div class="member-card-price">¥ {{ item.price }}</div>
      <p>有效期 {{ item.validDays }} 天｜等级 {{ item.levelCode }}</p>
      <div v-for="benefit in item.benefits" :key="benefit" class="member-benefit">{{ benefit }}</div>
      <el-tag v-if="item.recommended" type="warning">推荐</el-tag>
      <el-button
        class="full-button"
        type="primary"
        :disabled="!systemConfig.paymentEnabled"
        :loading="loadingPackageId === item.id"
        @click="handleBuy(item)"
      >
        {{ systemConfig.mockPaymentEnabled ? '模拟支付开通' : '创建订单' }}
      </el-button>
    </article>
  </section>

  <section class="card member-order-card">
    <h3>支付订单</h3>
    <el-table :data="orders" stripe style="width: 100%">
      <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
      <el-table-column prop="packageName" label="套餐" width="140" />
      <el-table-column prop="amount" label="金额" width="100" />
      <el-table-column prop="payChannel" label="支付方式" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PAID' ? 'success' : 'info'">{{ row.status === 'PAID' ? '已支付' : '待支付' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
    </el-table>
  </section>

  <MemberUpgradeDialog
    v-model:visible="visible"
    :packages="packages"
    :payment-enabled="systemConfig.paymentEnabled"
    :mock-payment-enabled="systemConfig.mockPaymentEnabled"
    :loading-package-id="loadingPackageId"
    @buy="handleBuy"
  />
</template>
