<!--
  后台系统配置 Tab
  功能：配置注册验证、登录限制、导出限制、会员支付开关
-->
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemConfig, updateSystemConfig } from '../../api/systemConfig'

const sysConfigLoading = ref(false)
const showEmailPassword = ref(false)
const sysConfig = reactive({
  registerEnabled: true,
  emailVerifyEnabled: false,
  emailUsername: '',
  emailPassword: '',
  singleIpEnabled: false,
  dailyExportLimit: 0,
  dailyAiLimit: 0,
  paymentEnabled: true,
  shopUrl: '',
  autoApproveArticle: false,
  communityApprovalRewardExportEnabled: false
})

const refresh = async () => {
  const config = await getSystemConfig()
  if (config) Object.assign(sysConfig, config)
}

onMounted(refresh)

const handleSave = async () => {
  sysConfigLoading.value = true
  try {
    await updateSystemConfig({ ...sysConfig })
    ElMessage.success('系统配置保存成功')
  } finally {
    sysConfigLoading.value = false
  }
}
</script>

<template>
  <section class="admin-panel-card card">
    <div class="admin-card-title">
      <h3>系统配置</h3>
      <span>控制注册、登录限制、导出限制和会员支付开关。</span>
    </div>

    <div class="sys-config-section">
      <div class="sys-config-header">
        <div>
          <h4>开放用户注册</h4>
          <p class="sys-config-desc">关闭后登录页将隐藏注册入口，且后端拒绝新用户注册。</p>
        </div>
        <el-switch v-model="sysConfig.registerEnabled" active-text="开放" inactive-text="关闭" />
      </div>
    </div>

    <div class="sys-config-section">
      <div class="sys-config-header">
        <div>
          <h4>邮箱验证注册</h4>
          <p class="sys-config-desc">开启后用户注册时必须使用邮箱验证码绑定账户。</p>
        </div>
        <el-switch v-model="sysConfig.emailVerifyEnabled" active-text="开启" inactive-text="关闭" />
      </div>
      <el-form v-if="sysConfig.emailVerifyEnabled" label-position="top" class="sys-config-form">
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
          <template #title>QQ 邮箱 SMTP 固定使用 smtp.qq.com:465 SSL，只需填写发送账号和授权码</template>
        </el-alert>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="发送邮箱账号">
              <el-input v-model="sysConfig.emailUsername" placeholder="your-email@qq.com" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱授权码">
              <el-input v-model="sysConfig.emailPassword" :type="showEmailPassword ? 'text' : 'password'" placeholder="在 QQ 邮箱设置中获取授权码">
                <template #suffix>
                  <button class="icon-text-button" type="button" @click="showEmailPassword = !showEmailPassword">
                    {{ showEmailPassword ? '隐藏' : '显示' }}
                  </button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <div class="sys-config-section">
      <div class="sys-config-header">
        <div>
          <h4>单 IP 登录限制</h4>
          <p class="sys-config-desc">开启后同一账号只能保留一个设备会话。</p>
        </div>
        <el-switch v-model="sysConfig.singleIpEnabled" active-text="开启" inactive-text="关闭" />
      </div>
    </div>

    <div class="sys-config-section">
      <div class="sys-config-header">
        <div>
          <h4>每日赠送次数（仅普通用户）</h4>
          <p class="sys-config-desc">每天赠送给免费用户的次数，当天有效、不累加到第二天；用完后可继续使用充值/奖励余额（永久有效）。会员的每日赠送由所购套餐配额决定，不受此处影响。<strong>-1 = 不限制；0 = 无赠送额度（须用额度兑换码获取次数）；N = 每天赠送 N 次</strong>。</p>
        </div>
      </div>
      <el-form label-position="top" class="sys-config-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="每日赠送导出次数">
              <el-input-number v-model="sysConfig.dailyExportLimit" :min="-1" :max="999" :step="1" />
              <span class="inline-help">{{ sysConfig.dailyExportLimit < 0 ? '不限制' : (sysConfig.dailyExportLimit === 0 ? '无赠送额度，需兑换码' : `普通用户每天赠送 ${sysConfig.dailyExportLimit} 次`) }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="每日赠送 AI 次数">
              <el-input-number v-model="sysConfig.dailyAiLimit" :min="-1" :max="999" :step="1" />
              <span class="inline-help">{{ sysConfig.dailyAiLimit < 0 ? '不限制' : (sysConfig.dailyAiLimit === 0 ? '无赠送额度，需兑换码' : `普通用户每天赠送 ${sysConfig.dailyAiLimit} 次`) }}</span>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <div class="sys-config-section">
      <div class="sys-config-header">
        <div>
          <h4>卡密购买（链动小铺）</h4>
          <p class="sys-config-desc">会员通过链动小铺购买卡密，再到会员中心兑换开通。关闭后会员页隐藏购买入口，仅保留兑换码。</p>
        </div>
        <el-switch v-model="sysConfig.paymentEnabled" active-text="显示购买入口" inactive-text="隐藏购买入口" />
      </div>
      <el-form label-position="top" class="sys-config-form">
        <el-form-item label="链动小铺店铺地址（购买入口跳转链接）">
          <el-input v-model="sysConfig.shopUrl" placeholder="https://pay.ldxp.cn/shop/AYCDCCFE" clearable>
            <template #prepend>🛒</template>
          </el-input>
          <span class="inline-help">会员页「前往小店购买卡密」按钮将跳转到此地址，更换店铺时改这里即可。</span>
        </el-form-item>
        <el-form-item v-if="sysConfig.shopUrl">
          <a :href="sysConfig.shopUrl" target="_blank" rel="noopener noreferrer" class="shop-preview-link">↗ 预览打开当前小店</a>
        </el-form-item>
      </el-form>
    </div>

    <div class="sys-config-section">
      <div class="sys-config-header">
        <div>
          <h4>优化技巧自动审批</h4>
          <p class="sys-config-desc">开启后用户投稿的优化技巧无需人工审核，提交即在社区长期展示；关闭后投稿进入待审核，由管理员在「社区管理」中通过。</p>
        </div>
        <el-switch v-model="sysConfig.autoApproveArticle" active-text="自动通过" inactive-text="人工审核" />
      </div>
    </div>

    <el-button type="primary" :loading="sysConfigLoading" @click="handleSave">保存配置</el-button>
  </section>
</template>

<style scoped>
.shop-preview-link {
  font-size: 13px;
  color: var(--el-color-primary);
  text-decoration: none;
}
.shop-preview-link:hover {
  text-decoration: underline;
}
</style>
