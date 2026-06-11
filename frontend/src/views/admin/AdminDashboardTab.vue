<!--
  后台统计概览 Tab
  功能：展示核心指标、近 7 日趋势和会员分布图表
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { getDashboard } from '../../api/admin'
import BaseChart from '../../components/common/BaseChart.vue'

const dashboard = ref(null)

const refresh = async () => {
  dashboard.value = await getDashboard()
}

onMounted(refresh)

const statCards = computed(() => {
  const data = dashboard.value || {}
  return [
    { label: '用户数', value: data.userCount || 0, desc: '平台注册账号', tone: 'blue' },
    { label: '简历数', value: data.resumeCount || 0, desc: '已创建简历', tone: 'green' },
    { label: '模板数', value: data.templateCount || 0, desc: '可用模板', tone: 'slate' },
    { label: '今日 AI', value: data.todayAiCalls || 0, desc: '优化调用', tone: 'amber' },
    { label: '会员用户', value: data.vipUserCount || 0, desc: '付费/权益用户', tone: 'purple' },
    { label: '套餐数', value: data.packageCount || 0, desc: '会员套餐', tone: 'teal' }
  ]
})

const axisStyle = {
  color: '#6e6e73',
  fontSize: 12
}

const lineOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 36, right: 18, top: 28, bottom: 28 },
  xAxis: {
    type: 'category',
    data: dashboard.value?.recentDates || [],
    axisLabel: axisStyle,
    axisLine: { lineStyle: { color: '#e5e7eb' } },
    axisTick: { show: false }
  },
  yAxis: {
    type: 'value',
    axisLabel: axisStyle,
    splitLine: { lineStyle: { color: '#f0f2f5' } }
  },
  series: [{
    name: 'AI 调用',
    type: 'line',
    smooth: true,
    symbolSize: 7,
    data: dashboard.value?.dailyAiCalls || [],
    lineStyle: { color: '#0071e3', width: 3 },
    itemStyle: { color: '#0071e3' },
    areaStyle: { color: 'rgba(0, 113, 227, 0.08)' }
  }]
}))

const barOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 36, right: 18, top: 28, bottom: 28 },
  xAxis: {
    type: 'category',
    data: dashboard.value?.recentDates || [],
    axisLabel: axisStyle,
    axisLine: { lineStyle: { color: '#e5e7eb' } },
    axisTick: { show: false }
  },
  yAxis: {
    type: 'value',
    axisLabel: axisStyle,
    splitLine: { lineStyle: { color: '#f0f2f5' } }
  },
  series: [{
    name: '新增用户',
    type: 'bar',
    data: dashboard.value?.dailyNewUsers || [],
    barWidth: 18,
    itemStyle: { color: '#34a853', borderRadius: [6, 6, 0, 0] }
  }]
}))

const pieOption = computed(() => {
  const labels = dashboard.value?.memberLevelLabels || ['免费用户', '会员用户']
  const values = dashboard.value?.memberLevelValues || [0, 0]
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: axisStyle },
    series: [{
      name: '会员分布',
      type: 'pie',
      radius: ['48%', '70%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 600 } },
      data: labels.map((name, index) => ({
        name,
        value: values[index] || 0,
        itemStyle: { color: index === 0 ? '#d8dee8' : '#0071e3' }
      }))
    }]
  }
})

const templateUsageOption = computed(() => {
  const labels = dashboard.value?.templateUsageLabels || []
  const values = dashboard.value?.templateUsageValues || []
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 24, top: 16, bottom: 8, containLabel: true },
    xAxis: {
      type: 'value',
      axisLabel: axisStyle,
      splitLine: { lineStyle: { color: '#f0f2f5' } }
    },
    yAxis: {
      type: 'category',
      data: [...labels].reverse(),
      axisLabel: axisStyle,
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false }
    },
    series: [{
      name: '使用次数',
      type: 'bar',
      data: [...values].reverse(),
      barWidth: 16,
      itemStyle: { color: '#8b5cf6', borderRadius: [0, 6, 6, 0] }
    }]
  }
})

const hasTemplateUsage = computed(() => (dashboard.value?.templateUsageValues || []).length > 0)
</script>

<template>
  <section v-if="dashboard" class="admin-dashboard-tab">
    <div class="admin-stat-grid">
      <article v-for="item in statCards" :key="item.label" class="admin-stat-card card" :class="`tone-${item.tone}`">
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
        <p>{{ item.desc }}</p>
      </article>
    </div>

    <div class="admin-chart-grid">
      <article class="admin-chart-card card">
        <div class="admin-card-title">
          <h3>近 7 日 AI 调用</h3>
          <span>观察优化功能使用热度</span>
        </div>
        <BaseChart :option="lineOption" />
      </article>
      <article class="admin-chart-card card">
        <div class="admin-card-title">
          <h3>近 7 日新增用户</h3>
          <span>用于验收演示的平滑趋势</span>
        </div>
        <BaseChart :option="barOption" />
      </article>
      <article class="admin-chart-card card admin-chart-card-wide">
        <div class="admin-card-title">
          <h3>会员用户占比</h3>
          <span>免费用户与会员用户构成</span>
        </div>
        <BaseChart :option="pieOption" height="300px" />
      </article>
      <article class="admin-chart-card card admin-chart-card-wide">
        <div class="admin-card-title">
          <h3>模板使用排行</h3>
          <span>简历引用次数 Top 5</span>
        </div>
        <BaseChart v-if="hasTemplateUsage" :option="templateUsageOption" height="280px" />
        <el-empty v-else description="暂无模板使用数据" />
      </article>
    </div>
  </section>
</template>
