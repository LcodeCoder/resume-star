/** 同题重练的表达覆盖量表。只衡量回答中是否显式说出四类信息，不推断能力。 */
const dimensions = [
  ['个人行动', /我(负责|设计|实现|开发|协调|验证|优化|提出)/],
  ['具体依据', /(代码|文档|测试|截图|链接|数据|日志|记录|原型)/],
  ['结果描述', /(结果|最终|提升|减少|完成|反馈|指标)/],
  ['复盘反思', /(复盘|改进|权衡|不足|下次|如果)/]
]
export const replayRubric = text => {
  const detail = dimensions.map(([label, pattern]) => ({ label, hit: pattern.test(String(text || '')), basis: String(text || '').match(pattern)?.[0] || '' }))
  return { score: detail.filter(item => item.hit).length * 25, detail }
}
