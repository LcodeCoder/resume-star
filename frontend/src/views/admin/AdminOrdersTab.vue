<!--
  后台订单与营收 Tab
  功能：展示营收概览指标、近 7 日营收趋势图和全部支付订单列表
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { getRevenue, listAdminOrders } from '../../api/admin'
import BaseChart from '../../components/common/BaseChart.vue'

const revenue = ref(null)
const orders = ref([])

const refresh = async () => {
  const [rev, orderList] = await Promise.all([getRevenue(), listAdminOrders()])
  revenue.value = rev
  orders.value = orderList || []
}

onMounted(refresh)

const statCards = computed(() => {
  const data = revenue.value || {}
  return [
    { label: '累计营收', value: `¥${Number(data.totalRevenue || 0).toFixed(2)}`, desc: '已支付订单总额', tone: 'green' },
    { label: '已支付订单', value: data.paidOrderCount || 0, desc: '成功付款笔数', tone: 'blue' },
    { label: '待支付订单', value: data.pendingOrderCount || 0, desc: '未完成付款', tone: 'amber' },
    { label: '订单总数', value: data.totalOrderCount || 0, desc: '全部订单', tone: 'slate' }
  ]
})

const axisStyle = { color: '#6e6e73', fontSize: 12 }

const revenueOption = computed(() => ({
  tooltip: { trigger: 'axis', valueFormatter: (v) => `¥${Number(v).toFixed(2)}` },
  grid: { left: 44, right: 18, top: 28, bottom: 28 },
  xAxis: {
    type: 'category',
    data: revenue.value?.recentDates || [],
    axisLabel: axisStyle,
    axisLine: { lineStyle: { color: '#e5e7eb' } },
    axisTick: { show: false }
  },
  yAxis: {
    type: 'value',
    axisLabel: { ...axisStyle, formatter: (v) => `¥${v}` },
    splitLine: { lineStyle: { color: '#f0f2f5' } }
  },
  series: [{
    name: '营收',
    type: 'bar',
    data: revenue.value?.dailyRevenue || [],
    barWidth: 22,
    itemStyle: { color: '#34a853', borderRadius: [6, 6, 0, 0] }
  }]
}))

const statusType = (status) => (status === 'PAID' ? 'success' : status === 'PENDING' ? 'warning' : 'info')
const statusLabel = (status) => (status === 'PAID' ? '已支付' : status === 'PENDING' ? '待支付' : status)
const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '—')
</script>

<template>
  <section class="admin-dashboard-tab">
    <div class="admin-stat-grid admin-stat-grid-4">
      <article v-for="item in statCards" :key="item.label" class="admin-stat-card card" :class="`tone-${item.tone}`">
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
        <p>{{ item.desc }}</p>
      </article>
    </div>

    <article class="admin-chart-card card">
      <div class="admin-card-title">
        <h3>近 7 日营收趋势</h3>
        <span>按订单支付时间统计</span>
      </div>
      <BaseChart :option="revenueOption" />
    </article>

    <section class="admin-panel-card card">
      <div class="admin-section-toolbar">
        <div>
          <h3>支付订单</h3>
          <p>全部会员购买订单及支付状态。</p>
        </div>
      </div>
      <el-table :data="orders" stripe style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="packageName" label="套餐" width="120" />
        <el-table-column prop="levelCode" label="等级" width="120" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }">¥{{ Number(row.amount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用户 ID" width="100" prop="userId" />
        <el-table-column label="下单时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="支付时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.paidTime) }}</template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>
