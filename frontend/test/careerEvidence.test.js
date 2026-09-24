import test from 'node:test'
import assert from 'node:assert/strict'
import { evidenceGraph, splitRequirements, sourcedResumeDraft } from '../src/utils/careerEvidence.js'
import { replayRubric } from '../src/utils/replayRubric.js'

test('逐条拆解 JD，空白与重复条目不进入图谱', () => {
  assert.deepEqual(splitRequirements('熟悉 Vue 3；熟悉 Vue 3。\n能设计接口'), ['熟悉 Vue 3', '能设计接口'])
})

test('关键词线索不自动形成证据链，用户确认的每段关系必须逐字存在', () => {
  const resume = { components: [{ id: 1, label: '项目经历', content: '我使用 Vue 3 开发页面' }] }
  const experiences = [{ id: 'exp1', title: '校园项目', description: '使用 Vue 3 开发页面', source: '项目日志：使用 Vue 3 开发页面' }]
  const answers = [{ id: 'a1', requirement: '熟悉 Vue 3', question: '怎么实现', answer: '我编写组件测试' }]
  const link = { experienceId: 'exp1', experienceQuote: '使用 Vue 3 开发页面', componentId: 1, resumeQuote: '我使用 Vue 3 开发页面', answerId: 'a1', answerQuote: '我编写组件测试' }
  const graph = evidenceGraph('熟悉 Vue 3；掌握 Redis', resume, experiences, answers)
  assert.equal(graph[0].evidence.length > 0, true)
  assert.equal(graph[0].chain.experience, null)
  assert.equal(graph[1].missing, true)
  const linked = evidenceGraph('熟悉 Vue 3', resume, experiences, answers, { '熟悉 Vue 3': link })[0]
  assert.equal(linked.chain.answer.quote, '我编写组件测试')
  assert.equal(evidenceGraph('熟悉 Vue 3', resume, experiences, answers, { '熟悉 Vue 3': { ...link, resumeQuote: '不存在' } })[0].chain.answer, null)
  assert.equal(evidenceGraph('熟悉 Vue 3', { components: [] }, experiences, answers, { '熟悉 Vue 3': link })[0].chain.resume, null)
})

test('简历候选句只保留材料原文，不能补出不存在的数字', () => {
  const e = { title: '校园项目', source: '工作日志：编写组件测试。' }
  assert.equal(sourcedResumeDraft(e, '编写组件测试').quote, '编写组件测试')
  assert.equal(sourcedResumeDraft(e, '提升 50%'), null)
})

test('两次回答均按同一量表返回得分和命中依据', () => {
  assert.equal(replayRubric('我负责代码测试，最终完成，复盘时发现不足').score, 100)
  assert.deepEqual(replayRubric('我负责页面').detail[0], { label: '个人行动', hit: true, basis: '我负责' })
  assert.equal(replayRubric('我负责页面').score, 25)
})

test('核验记录的逐字引用可形成经历关系，不会丢失推荐出处', () => {
  const graph = evidenceGraph('高并发接口', null,
    [{ id: 'p1', title: '限流实践', description: '我调优缓存', evidence: '压测峰值每秒300次' }],
    [], { '高并发接口': { experienceId: 'p1', experienceQuote: '压测峰值每秒300次' } })
  assert.equal(graph[0].chain.experience.quote, '压测峰值每秒300次')
})
