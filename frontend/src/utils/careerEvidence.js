/** 岗位匹配仅提供线索；只有用户明确建立且来源原文仍存在的关系才能进入证据链。 */
export const splitRequirements = (jd) => [...new Set(String(jd || '')
  .split(/[\n；;。•]/)
  .map(s => s.replace(/^[\s\d、.（）()\-]+/, '').trim())
  .filter(s => s.length >= 3))].slice(0, 16)

export const flattenResume = (resume) => (resume?.components || [])
  .filter(c => typeof c.content === 'string' && c.content.trim())
  .map(c => ({ id: c.id, label: c.label || c.type || '简历内容', text: c.content.trim() }))

const tokens = (text) => (String(text || '').toLowerCase().match(/[a-z][a-z0-9+.#-]{1,}|[\u4e00-\u9fff]{2,}/g) || [])
  .flatMap(t => /[\u4e00-\u9fff]/.test(t) && t.length > 4 ? [t, ...Array.from({ length: t.length - 1 }, (_, i) => t.slice(i, i + 2))] : [t])
  .filter(t => !['要求', '负责', '具有', '能够', '熟悉', '相关', '工作', '经验', '能力'].includes(t))

export const exactExcerpt = (source, excerpt) => {
  const quote = String(excerpt || '').trim()
  return quote.length >= 3 && String(source || '').includes(quote)
}

/** 返回逐字引用的候选句。数字只能从材料原文提取，不能由模型或用户描述补出。 */
export const sourcedResumeDraft = (experience, excerpt) => {
  const quote = String(excerpt || '').trim()
  if (!exactExcerpt(experience?.source, quote)) return null
  return { text: `项目经历：${experience.title || '未命名项目'}。材料原文：“${quote}”`, quote, source: experience.source }
}

export const evidenceGraph = (jd, resume, experiences = [], answers = [], links = {}) => {
  const resumeSources = flattenResume(resume)
  const sources = [
    ...resumeSources.map(s => ({ ...s, source: '简历' })),
    ...experiences.map(s => ({ id: s.id, label: s.title || '项目经历', text: s.description || '', source: '经历库' }))
  ]
  return splitRequirements(jd).map((requirement, index) => {
    const words = [...new Set(tokens(requirement))]
    const ranked = sources.map(item => ({ ...item, overlap: words.filter(w => item.text.toLowerCase().includes(w)) }))
      .filter(item => item.overlap.length).sort((a, b) => b.overlap.length - a.overlap.length).slice(0, 3)
    const related = answers.filter(a => a.requirement === requirement).map(a => ({ id: a.id, question: a.question, answer: a.answer }))
    const link = links?.[requirement]
    const experience = experiences.find(e => String(e.id) === String(link?.experienceId))
    const resumePart = resumeSources.find(e => String(e.id) === String(link?.componentId))
    const linkedAnswer = related.find(a => String(a.id) === String(link?.answerId))
    const chain = {
      experience: experience && exactExcerpt(`${experience.description || ''}\n${experience.source || ''}`, link.experienceQuote)
        ? { ...experience, quote: link.experienceQuote.trim() } : null,
      resume: resumePart && exactExcerpt(resumePart.text, link.resumeQuote)
        ? { ...resumePart, quote: link.resumeQuote.trim() } : null,
      answer: linkedAnswer && exactExcerpt(linkedAnswer.answer, link.answerQuote)
        ? { ...linkedAnswer, quote: link.answerQuote.trim() } : null
    }
    // 下游关系必须依赖上游关系，避免不同项目的关键词命中看似形成完整链条。
    if (!chain.experience) { chain.resume = null; chain.answer = null }
    else if (!chain.resume) chain.answer = null
    return { id: `req-${index}`, requirement, evidence: ranked, answers: related, chain, missing: !chain.experience }
  })
}
