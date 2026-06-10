<!--
  会员升级弹窗组件
  页面位置：AI功能、会员模板点击触发
  功能：展示会员套餐与权益，并根据后台支付开关提供模拟支付入口
-->
<script setup>
const props = defineProps({
  visible: Boolean,
  packages: {
    type: Array,
    default: () => []
  },
  paymentEnabled: Boolean,
  mockPaymentEnabled: Boolean,
  loadingPackageId: Number
})

const emit = defineEmits(['update:visible', 'buy'])

const close = () => emit('update:visible', false)
const buy = (item) => emit('buy', item)
</script>

<template>
  <el-dialog :model-value="props.visible" title="升级会员" width="720px" @close="close">
    <el-alert
      v-if="!props.paymentEnabled"
      type="info"
      :closable="false"
      show-icon
      title="管理员暂未开启支付功能，当前仅展示套餐权益。"
      style="margin-bottom: 16px"
    />
    <div class="member-grid">
      <div v-for="item in props.packages" :key="item.id" class="member-card card">
        <div class="member-card-title">{{ item.name }}</div>
        <div class="member-card-price">¥ {{ item.price }}</div>
        <div class="member-card-days">有效期 {{ item.validDays }} 天</div>
        <div class="member-benefit" v-for="benefit in item.benefits" :key="benefit">{{ benefit }}</div>
        <el-tag v-if="item.recommended" type="warning">推荐</el-tag>
        <el-button
          class="full-button"
          type="primary"
          :disabled="!props.paymentEnabled"
          :loading="props.loadingPackageId === item.id"
          @click="buy(item)"
        >
          {{ props.mockPaymentEnabled ? '模拟支付开通' : '创建订单' }}
        </el-button>
      </div>
    </div>
    <template #footer>
      <el-button @click="close">稍后再说</el-button>
    </template>
  </el-dialog>
</template>
