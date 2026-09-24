import test from 'node:test'
import assert from 'node:assert/strict'
import { createInterviewerSession, candidateLabel, localCandidateReply, validateInterviewerDecision, interviewerReview } from '../src/utils/interviewerChallenge.js'

test('匿名顺序随机交换，事实身份保持不变', () => {
  const first = createInterviewerSession(() => 0.1)
  const second = createInterviewerSession(() => 0.9)
  assert.deepEqual(first.order, ['grounded', 'polished'])
  assert.deepEqual(second.order, ['polished', 'grounded'])
  assert.equal(candidateLabel(second, 'grounded'), 'B')
})

test('本地演练的两个候选人根据追问披露证据边界', () => {
  assert.match(localCandidateReply('grounded', '有哪些证据？', 1), /前后版本/)
  assert.match(localCandidateReply('polished', '有哪些证据？', 1), /没有留存/)
})

test('至少三轮且引用必须逐字出自选中的候选人', () => {
  const session = createInterviewerSession(() => 0)
  const turn = { question: '你做了什么？', answers: { grounded: '我修改了报名表，并保留修改前后版本。', polished: '我们推广做得很好。' }, sources: { grounded: 'local', polished: 'local' } }
  session.turns.push(turn, turn)
  assert.match(validateInterviewerDecision(session, 'grounded', '我看到了可核实的证据和个人分工', '修改前后版本'), /至少两轮/)
  session.turns.push(turn)
  assert.match(validateInterviewerDecision(session, 'grounded', '我看到了可核实的证据和个人分工', '推广做得很好'), /选择的候选人/)
  assert.equal(validateInterviewerDecision(session, 'grounded', '我看到了可核实的证据和个人分工', '修改前后版本'), null)
  session.decision = { selected: 'grounded', reason: '我看到了可核实的证据和个人分工', quote: '修改前后版本' }
  assert.equal(interviewerReview(session).checkedEvidence, true)
  assert.equal(interviewerReview(session).checkedContribution, true)
})
