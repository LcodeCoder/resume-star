<!--
  会员中心页面
  功能：展示当前会员状态、会员套餐（定价卡）、兑换码开通入口、模拟支付与订单记录
  设计：渐变主视觉 + 定价卡（推荐款高亮）+ 兑换码行动区 + 订单表
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listMemberPackages, listQuotaPackages, redeemMembership } from '../api/member'
import { getUserSystemConfig } from '../api/user'
import { useUserStore } from '../store/user'
import MemberUpgradeDialog from '../components/member-tip/MemberUpgradeDialog.vue'

const DEFAULT_SHOP_URL = 'https://pay.ldxp.cn/shop/AYCDCCFE'

const userStore = useUserStore()
const packages = ref([])
const quotaPackages = ref([])
const visible = ref(false)
const systemConfig = ref({ paymentEnabled: true, shopUrl: DEFAULT_SHOP_URL })
/** 兑换码输入与提交状态 */
const redeemCode = ref('')
const redeeming = ref(false)

/** 链动小铺购买地址（管理员可在后台配置） */
const shopUrl = computed(() => systemConfig.value.shopUrl || DEFAULT_SHOP_URL)
/** 是否展示购买入口（由后台支付设置控制） */
const showBuyEntry = computed(() => systemConfig.value.paymentEnabled !== false)

/** 会员等级编码 → 中文名 */
const LEVEL_LABELS = { FREE: '免费版', BASIC: '基础会员', PRO: '专业会员', ENTERPRISE: '企业会员' }
const levelLabel = (code) => LEVEL_LABELS[code] || code

/** 当前是否为付费会员 */
const isVip = computed(() => !!userStore.vipLevel)
/** 会员到期时间（YYYY-MM-DD） */
const expireText = computed(() => {
  const t = userStore.profile?.vipExpireTime
  return t ? String(t).replace('T', ' ').slice(0, 10) : ''
})

/** 套餐特性列表：优先用配置的权益文案，缺省时按额度自动生成 */
const planFeatures = (item) => {
  if (item.benefits && item.benefits.length) return item.benefits
  const list = []
  if (item.dailyAiQuota != null) list.push(`每日 AI ${item.dailyAiQuota} 次`)
  if (item.dailyExportQuota != null) list.push(`每日导出 ${item.dailyExportQuota} 次`)
  if (item.dailyInterviewQuota != null && item.dailyInterviewQuota > 0) list.push(`每日模拟面试 ${item.dailyInterviewQuota} 次`)
  list.push('全部高级模板', '云端存储与版本历史')
  return list
}

/** 额度套餐特性列表：优先用配置文案，缺省按赠送次数自动生成（充值卡，用完为止） */
const quotaFeatures = (item) => {
  if (item.benefits && item.benefits.length) return item.benefits
  const list = []
  if (item.aiCount) list.push(`AI 次数 +${item.aiCount}`)
  if (item.exportCount) list.push(`导出次数 +${item.exportCount}`)
  if (item.interviewCount) list.push(`模拟面试 +${item.interviewCount}`)
  list.push('永久有效，用完为止')
  return list
}

const refresh = async () => {
  await userStore.loadProfile()
  const [packageList, quotaList, config] = await Promise.all([
    listMemberPackages(),
    listQuotaPackages(),
    getUserSystemConfig()
  ])
  packages.value = packageList
  quotaPackages.value = quotaList || []
  systemConfig.value = config || systemConfig.value
}

onMounted(refresh)

/** 前往链动小铺购买卡密 */
const openShop = () => {
  if (!showBuyEntry.value) {
    ElMessage.warning('管理员暂未开放购买入口，可使用下方兑换码开通')
    return
  }
  window.open(shopUrl.value, '_blank', 'noopener')
}

/** 使用兑换码开通会员 */
const handleRedeem = async () => {
  if (!redeemCode.value.trim()) {
    ElMessage.warning('请输入兑换码')
    return
  }
  redeeming.value = true
  try {
    const levelName = await redeemMembership({ code: redeemCode.value.trim(), userId: userStore.profile?.id || 1 })
    ElMessage.success(`兑换成功：${levelName}`)
    redeemCode.value = ''
    await Promise.all([userStore.loadProfile(), userStore.loadQuota()])
  } finally {
    redeeming.value = false
  }
}
</script>

<template>
  <!-- 主视觉：左侧文案 + 右侧当前会员状态卡 -->
  <section class="member-hero">
    <div class="member-hero-decor" aria-hidden="true"></div>
    <div class="member-hero-main">
      <div class="member-hero-text">
        <div class="member-hero-badge">会员中心</div>
        <h2>升级会员，解锁更强的简历能力</h2>
        <p>获得更多 AI 优化与导出额度，畅享全部高级模板与专属权益。</p>
        <div class="payment-status-row">
          <span class="pay-pill on">🛒 卡密购买 · 链动小铺</span>
          <button v-if="showBuyEntry" class="member-hero-link" @click="openShop">前往小店购买卡密 →</button>
          <button class="member-hero-link" @click="visible = true">查看套餐对比 →</button>
        </div>
      </div>

      <!-- 当前会员状态卡 -->
      <div class="member-status-card" :class="{ vip: isVip }">
        <div class="status-top">
          <span class="status-crown">{{ isVip ? '👑' : '✦' }}</span>
          <div>
            <div class="status-level">{{ userStore.vipLevelLabel || levelLabel(userStore.vipLevel) }}</div>
            <div class="status-expire">{{ isVip && expireText ? '有效期至 ' + expireText : '当前为免费版' }}</div>
          </div>
        </div>
        <div class="status-quota-group">
          <div class="status-group-label">今日额度</div>
          <div class="status-quotas">
            <div class="status-quota">
              <strong>{{ userStore.remainingAiQuota }}</strong>
              <span>AI 剩余次数</span>
            </div>
            <div class="status-quota">
              <strong>{{ userStore.remainingExportQuota }}</strong>
              <span>导出剩余次数</span>
            </div>
          </div>
        </div>
        <div class="status-quota-group">
          <div class="status-group-label">充值余额（用完为止）</div>
          <div class="status-quotas">
            <div class="status-quota">
              <strong>{{ userStore.aiBalance }}</strong>
              <span>AI 次数</span>
            </div>
            <div class="status-quota">
              <strong>{{ userStore.exportBalance }}</strong>
              <span>导出次数</span>
            </div>
            <div class="status-quota">
              <strong>{{ userStore.interviewBalance }}</strong>
              <span>面试次数</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>

  <el-alert
    v-if="!showBuyEntry"
    class="member-buy-alert"
    type="info"
    :closable="false"
    show-icon
    title="管理员暂未开放购买入口，可使用下方兑换码直接开通会员。"
  />

  <!-- 套餐定价卡 -->
  <section class="plan-grid">
    <article
      v-for="item in packages"
      :key="item.id"
      class="plan-card card"
      :class="{ featured: item.recommended }"
    >
      <div v-if="item.recommended" class="plan-ribbon">最受欢迎</div>
      <div class="plan-head">
        <h3 class="plan-name">{{ item.name }}</h3>
        <span class="plan-level-tag">{{ levelLabel(item.levelCode) }}</span>
      </div>
      <div class="plan-price">
        <span class="plan-currency">¥</span>
        <span class="plan-amount">{{ item.price }}</span>
        <span class="plan-period">/ {{ item.validDays }} 天</span>
      </div>
      <ul class="plan-benefits">
        <li v-for="benefit in planFeatures(item)" :key="benefit">
          <svg class="plan-check" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><polyline points="4 10.5 8.5 15 16 5"/></svg>
          <span>{{ benefit }}</span>
        </li>
      </ul>
      <el-button
        class="plan-cta"
        :type="item.recommended ? 'primary' : 'default'"
        :disabled="!showBuyEntry"
        @click="openShop"
      >
        前往小店购买卡密
      </el-button>
    </article>
  </section>

  <!-- 次数充值套餐（充值卡，用完为止） -->
  <section v-if="quotaPackages.length" class="quota-section">
    <div class="quota-section-head">
      <div>
        <h3>次数充值套餐</h3>
        <p>一次性「次数包」（充值卡）：兑换后把 AI / 导出 / 模拟面试次数累加到余额，跨日保留、用完为止。</p>
      </div>
    </div>
    <div class="plan-grid">
      <article
        v-for="item in quotaPackages"
        :key="'quota-' + item.id"
        class="plan-card card quota-card"
        :class="{ featured: item.recommended }"
      >
        <div v-if="item.recommended" class="plan-ribbon">推荐</div>
        <div class="plan-head">
          <h3 class="plan-name">{{ item.name }}</h3>
          <span class="plan-level-tag quota-tag">次数包</span>
        </div>
        <div class="plan-price">
          <span class="plan-currency">¥</span>
          <span class="plan-amount">{{ item.price }}</span>
          <span class="plan-period">/ 永久</span>
        </div>
        <ul class="plan-benefits">
          <li v-for="benefit in quotaFeatures(item)" :key="benefit">
            <svg class="plan-check" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><polyline points="4 10.5 8.5 15 16 5"/></svg>
            <span>{{ benefit }}</span>
          </li>
        </ul>
        <el-button
          class="plan-cta"
          :type="item.recommended ? 'primary' : 'default'"
          :disabled="!showBuyEntry"
          @click="openShop"
        >
          前往小店购买卡密
        </el-button>
      </article>
    </div>
  </section>

  <!-- 兑换码开通：无需支付 -->
  <section class="redeem-band">
    <div class="redeem-decor" aria-hidden="true"></div>
    <div class="redeem-inner">
      <div class="redeem-icon">🎁</div>
      <div class="redeem-text">
        <h3>使用兑换码开通会员</h3>
        <p>输入管理员发放的兑换码（邀请码），无需支付即可直接开通对应会员。</p>
        <a
          class="redeem-shop-link"
          :href="shopUrl"
          target="_blank"
          rel="noopener noreferrer"
        >还没有卡密？前往小店购买 →</a>
      </div>
      <div class="redeem-form">
        <el-input
          v-model="redeemCode"
          class="redeem-input"
          placeholder="请输入兑换码，如 RL-XXX-XXX-XXX"
          clearable
          size="large"
          @keyup.enter="handleRedeem"
        />
        <el-button type="primary" size="large" :loading="redeeming" @click="handleRedeem">立即兑换</el-button>
      </div>
    </div>
  </section>

  <MemberUpgradeDialog
    v-model:visible="visible"
    :packages="packages"
    :payment-enabled="showBuyEntry"
    :shop-url="shopUrl"
    @buy="openShop"
  />
</template>

<style scoped>
/* 次数充值套餐区 */
.quota-section {
  margin-top: 32px;
}
.quota-section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 14px;
  padding: 0 4px;
}
.quota-section-head h3 {
  margin: 0;
  font-size: 20px;
  color: #1f1f23;
}
.quota-section-head p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6e6e73;
}
.quota-tag {
  background: #fff4e6;
  color: #b9770a;
}

/* 小店购买入口：与兑换区文案并列展示 */
.redeem-shop-link {
  display: inline-block;
  margin-top: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  text-decoration: underline;
  text-underline-offset: 3px;
  opacity: 0.92;
  transition: opacity 0.2s ease;
}
.redeem-shop-link:hover {
  opacity: 1;
}
</style>
