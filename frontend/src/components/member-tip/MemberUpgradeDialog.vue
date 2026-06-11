<!--
  会员升级弹窗组件
  页面位置：AI功能、会员模板点击触发
  功能：展示会员套餐与权益，购买入口跳转到链动小铺小店购买卡密，回站内兑换开通
-->
<script setup>
const props = defineProps({
  visible: Boolean,
  packages: {
    type: Array,
    default: () => []
  },
  paymentEnabled: Boolean,
  shopUrl: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:visible'])

const close = () => emit('update:visible', false)
/** 跳转链动小铺购买卡密 */
const buy = () => {
  const url = props.shopUrl || 'https://pay.ldxp.cn/shop/AYCDCCFE'
  window.open(url, '_blank', 'noopener')
}
</script>

<template>
  <el-dialog :model-value="props.visible" title="升级会员" width="720px" @close="close">
    <el-alert
      v-if="!props.paymentEnabled"
      type="info"
      :closable="false"
      show-icon
      title="管理员暂未开放购买入口，可使用兑换码开通会员。"
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
          @click="buy(item)"
        >
          前往小店购买卡密
        </el-button>
      </div>
    </div>
    <template #footer>
      <el-button @click="close">稍后再说</el-button>
    </template>
  </el-dialog>
</template>
