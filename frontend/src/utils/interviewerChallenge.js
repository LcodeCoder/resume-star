/** 固定的教学场景。候选人及其材料均为虚构，不代表真实求职者。 */
export const interviewerScenario = {
  role: '校园产品运营实习生',
  task: '为校园活动报名流程减少临时弃单，并说明个人贡献与验证办法。',
  cards: {
    grounded: {
      profile: '参与校内社团的活动报名优化，负责报名表和提醒文案。',
      facts: ['本人整理了 32 份报名反馈和弃单原因', '本人调整了报名表字段，并保留修改前后版本', '活动现场名单和群内提醒截图可用于交叉核对；没有可靠的转化率统计']
    },
    polished: {
      profile: '参与同一类校园活动的报名推广，负责沟通与活动宣传。',
      facts: ['有活动海报和群消息记录', '没有可供核对的个人分工记录', '没有统计过报名转化，无法独立证明团队效果归因']
    }
  }
}

export const createInterviewerSession = (random = Math.random) => ({
  id: crypto.randomUUID(),
  role: interviewerScenario.role,
  order: random() < 0.5 ? ['grounded', 'polished'] : ['polished', 'grounded'],
  turns: [],
  decision: null,
  createdAt: new Date().toISOString()
})

export const candidateLabel = (session, kind) => session.order.indexOf(kind) === 0 ? 'A' : 'B'

export function localCandidateReply(kind, question, round) {
  const evidence = /证据|材料|截图|核实|数据|记录|证明|依据/.test(question)
  const contribution = /你|本人|负责|个人|亲自|具体|分工|行动/.test(question)
  if (kind === 'grounded') {
    if (evidence) return '我保留了报名表的前后版本、32 份反馈的分类表及群内提醒截图；这些材料能证明我改了什么，但不能单凭它们证明转化率提升。'
    if (contribution) return '我本人整理了 32 份反馈，发现表单字段不清楚，随后修改了字段和提醒文案。现场名单由社团负责人维护，不是我个人的成果。'
    return round === 0 ? '我参与了社团报名优化，亲自整理 32 份反馈并修改表单字段。结果没有做可靠的转化统计，我可以展示修改前后的版本。' : '我会先记录弃单原因，再做一版字段修改；如果要说效果，得先约定统计口径并保留对照数据。'
  }
  if (evidence) return '目前只有活动海报和群消息，个人分工与转化率没有留存可核对的记录。我之前把团队成绩说成个人推动，表述需要收窄。'
  if (contribution) return '我主要帮团队做宣传和沟通，具体是谁负责报名表优化没有留分工记录；我不能独立证明整场活动的效果来自我。'
  return round === 0 ? '我们做了很成功的校园活动推广，我积极协调资源、推动大家配合，整体效果很不错。' : '我会继续推动团队协作和资源整合，让活动产生更大的影响；具体数据目前没有单独统计。'
}

export function validateInterviewerDecision(session, selected, reason, quote) {
  if (!session || session.turns.length < 3) return '请先完成首次提问和至少两轮追问'
  if (!session.order.includes(selected)) return '请选择候选人 A 或 B'
  if (reason.trim().length < 10) return '请写出至少 10 个字的判断理由'
  if (quote.trim().length < 4) return '请引用至少 4 个字的候选人回答原句'
  if (!session.turns.some(turn => turn.answers?.[selected]?.includes(quote.trim()))) return '引用必须逐字出现在你选择的候选人回答中'
  return null
}

export function interviewerReview(session) {
  const { selected, reason } = session.decision
  const checkedEvidence = /证据|材料|截图|记录|版本|核实|依据/.test(reason)
  const checkedContribution = /本人|个人|亲自|分工|负责|行动|贡献/.test(reason)
  return {
    checkedEvidence,
    checkedContribution,
    feedback: selected === 'grounded'
      ? '你的选择更容易追溯到个人行动和材料。仍应核对材料原件，并询问统计口径，模拟回答不能证明实际工作能力。'
      : '你选择的候选人表达积极，但个人分工和效果尚无可核对材料。可以继续索要工作记录并追问数据口径，再作判断。'
  }
}
