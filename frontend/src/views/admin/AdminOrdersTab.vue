<!--
  后台营收 Tab
  功能：展示卡密营收概览指标与近 7 日营收趋势（按已兑换卡密统计，金额取所绑套餐价）
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { getRevenue } from '../../api/admin'
import BaseChart from '../../components/common/BaseChart.vue'

const revenue = ref(null)

const refresh = async () => {
  revenue.value = await getRevenue()
}

onMounted(refresh)

const statCards = computed(() => {
  const data = revenue.value || {}
  return [
    { label: '累计营收', value: `¥${Number(data.totalRevenue || 0).toFixed(2)}`, desc: '已兑换卡密总额', tone: 'green' },
    { label: '已兑换卡密', value: data.paidOrderCount || 0, desc: '已使用卡密数', tone: 'blue' },
    { label: '未使用卡密', value: data.pendingOrderCount || 0, desc: '待兑换库存', tone: 'amber' },
    { label: '卡密总数', value: data.totalOrderCount || 0, desc: '全部卡密', tone: 'slate' }
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
        <span>按卡密兑换时间统计（金额取所绑套餐价）</span>
      </div>
      <BaseChart :option="revenueOption" />
    </article>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="营收按已兑换的卡密统计；卡密的发放与使用明细见「会员管理 → 会员兑换码」。"
      style="border-radius: 12px"
    />
  </section>
</template>
