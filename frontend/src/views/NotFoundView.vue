<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <section class="lost-sector">
    <div class="lost-map" aria-hidden="true"><span></span><i></i><b>404</b></div>
    <div><span class="lost-code">SIGNAL LOST · 404</span><h1>这条航线没有目的地。</h1><p>页面可能已经移动，或链接缺少一段坐标。返回任务总览可以继续编辑，已有内容不会受到影响。</p><div class="lost-actions"><el-button type="primary" @click="router.push('/')">返回任务总览</el-button><el-button @click="router.back()">回到上一页</el-button></div></div>
  </section>
</template>

<style scoped>
.lost-sector{min-height:calc(100vh - 220px);display:grid;grid-template-columns:minmax(280px,.8fr) 1fr;align-items:center;gap:8vw}.lost-map{position:relative;aspect-ratio:1;display:grid;place-items:center;border:1px solid var(--line);border-radius:50%}.lost-map:before,.lost-map:after{content:"";position:absolute;width:75%;height:35%;border:1px solid var(--line);border-radius:50%;transform:rotate(22deg)}.lost-map:after{transform:rotate(-32deg)}.lost-map span{width:35%;height:35%;border-radius:50%;background:var(--surface-2);box-shadow:inset 0 0 0 1px var(--line)}.lost-map i{position:absolute;right:12%;top:29%;width:13px;height:13px;border-radius:50%;background:var(--danger)}.lost-map b{position:absolute;font:800 52px/1 ui-monospace,monospace;color:var(--ink)}.lost-code{color:var(--danger);font:750 10px/1.4 ui-monospace,monospace;letter-spacing:.14em}.lost-sector h1{max-width:11ch;margin:20px 0;font-size:44px;line-height:1.1;letter-spacing:-.04em}.lost-sector p{max-width:52ch;color:var(--ink-2);line-height:1.8}.lost-actions{display:flex;gap:10px;margin-top:28px}@media(max-width:700px){.lost-sector{grid-template-columns:1fr}.lost-map{width:220px}.lost-sector h1{font-size:34px}}
</style>
