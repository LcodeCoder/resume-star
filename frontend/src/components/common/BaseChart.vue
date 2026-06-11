<!--
  ECharts 基础图表组件
  功能：统一初始化、更新、ResizeObserver 自适应和销毁，保持后台图表轻量可复用
-->
<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: {
    type: Object,
    required: true
  },
  height: {
    type: String,
    default: '280px'
  }
})

const chartRef = ref(null)
let chart = null
let resizeObserver = null

const renderChart = () => {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption(props.option, true)
}

onMounted(() => {
  renderChart()
  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartRef.value)
})

watch(() => props.option, renderChart, { deep: true })

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div ref="chartRef" class="base-chart" :style="{ height }"></div>
</template>
