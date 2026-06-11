<!--
  法律协议查看弹窗
  功能：以标签页形式展示《用户服务协议》《隐私政策》《会员服务协议》
  使用位置：登录/注册页勾选项、页脚链接
-->
<script setup>
import { computed, ref, watch } from 'vue'
import { LEGAL_DOCS, LEGAL_UPDATED } from '../../data/legalDocs'

const props = defineProps({
  /** 弹窗显隐（v-model:visible） */
  visible: { type: Boolean, default: false },
  /** 初始展示的协议 key：terms | privacy | membership */
  doc: { type: String, default: 'terms' }
})
const emit = defineEmits(['update:visible'])

const docs = LEGAL_DOCS
const active = ref(props.doc)

// 外部指定要看的协议时同步切换
watch(() => props.doc, (val) => { if (val) active.value = val })
watch(() => props.visible, (val) => { if (val) active.value = props.doc || 'terms' })

const current = computed(() => docs.find((d) => d.key === active.value) || docs[0])

const close = () => emit('update:visible', false)
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="法律条款与协议"
    width="720px"
    top="6vh"
    @update:model-value="close"
  >
    <div class="legal-dialog">
      <!-- 左侧协议切换 -->
      <aside class="legal-nav">
        <button
          v-for="d in docs"
          :key="d.key"
          class="legal-nav-item"
          :class="{ active: active === d.key }"
          @click="active = d.key"
        >
          {{ d.title }}
        </button>
        <p class="legal-updated">更新日期：{{ LEGAL_UPDATED }}</p>
      </aside>

      <!-- 右侧协议正文 -->
      <article class="legal-body">
        <h3 class="legal-title">{{ current.title }}</h3>
        <p class="legal-intro">{{ current.intro }}</p>
        <section v-for="sec in current.sections" :key="sec.h" class="legal-section">
          <h4>{{ sec.h }}</h4>
          <p v-for="(item, idx) in sec.items" :key="idx" class="legal-clause">{{ item }}</p>
        </section>
        <p class="legal-disclaimer">
          本协议为通用模板，正式商用前请补全运营方信息并咨询执业律师，以符合相关法律法规要求。
        </p>
      </article>
    </div>

    <template #footer>
      <el-button type="primary" @click="close">我已了解</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.legal-dialog {
  display: grid;
  grid-template-columns: 160px 1fr;
  gap: 20px;
  max-height: 64vh;
}

.legal-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  border-right: 1px solid rgba(0, 0, 0, 0.06);
  padding-right: 12px;
}

.legal-nav-item {
  text-align: left;
  border: none;
  background: transparent;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 14px;
  color: #424245;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.legal-nav-item:hover {
  background: #f5f5f7;
}

.legal-nav-item.active {
  background: #eef5ff;
  color: #0071e3;
  font-weight: 600;
}

.legal-updated {
  margin-top: auto;
  padding: 12px 12px 0;
  font-size: 12px;
  color: #9e9ea4;
}

.legal-body {
  overflow-y: auto;
  padding-right: 6px;
  max-height: 64vh;
}

.legal-title {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 700;
  color: #1d1d1f;
}

.legal-intro {
  margin: 0 0 18px;
  font-size: 13px;
  line-height: 1.8;
  color: #6e6e73;
}

.legal-section {
  margin-bottom: 18px;
}

.legal-section h4 {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
}

.legal-clause {
  margin: 0 0 7px;
  font-size: 13px;
  line-height: 1.75;
  color: #424245;
}

.legal-disclaimer {
  margin-top: 24px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #fff8ec;
  border: 1px solid rgba(245, 158, 11, 0.2);
  font-size: 12px;
  line-height: 1.7;
  color: #9a6a00;
}

@media (max-width: 768px) {
  .legal-dialog {
    grid-template-columns: 1fr;
  }

  .legal-nav {
    flex-direction: row;
    flex-wrap: wrap;
    border-right: none;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    padding-right: 0;
    padding-bottom: 10px;
  }

  .legal-updated {
    width: 100%;
  }
}
</style>
