<!--
  应用根组件
  功能：承载全局布局、路由视图，以及顶部进度条 + Lcode 拼字加载遮罩
-->
<script setup>
import GlobalLoader from './components/common/GlobalLoader.vue'
</script>

<template>
  <router-view />
  <GlobalLoader />
</template>
