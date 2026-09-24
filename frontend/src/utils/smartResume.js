import { layoutStructuredResume, defaultPageStyle } from './resumeLayout.js'

const text = value => typeof value === 'string' ? value.trim() : ''
const normalize = value => text(value).replace(/[\s·（）()]/g, '')
const sectionAliases = [
  ['教育背景', '教育经历', '学历'],
  ['专业技能', '技能特长', '技术栈', '技能'],
  ['项目经历', '项目经验', '项目实践'],
  ['实习经历', '工作经历', '工作经验'],
  ['校园经历', '校园实践', '社团经历', '社团活动', '志愿服务'],
  ['获奖证书', '奖项证书', '证书荣誉', '获奖经历', '证书资质'],
  ['自我评价', '个人优势', '个人总结', '个人简介']
]
const canonical = value => {
  const label = normalize(value)
  return sectionAliases.find(group => group.some(alias => label.includes(alias)))?.[0] || label
}

export const validateGeneratedResume = raw => {
  const data = raw?.resume || raw
  if (!data || !Array.isArray(data.sections)) throw new Error('AI 未返回可用的简历结构')
  const sections = data.sections.map(item => ({ title: text(item?.title), body: text(item?.body) }))
    .filter(item => item.title && item.body)
  if (!sections.length) throw new Error('AI 未返回有效经历，请补充真实材料后重试')
  return {
    name: text(data.name), title: text(data.title),
    contacts: (Array.isArray(data.contacts) ? data.contacts : []).map(item => ({ label: text(item?.label), content: text(item?.content) })).filter(item => item.content),
    sections
  }
}

/** 有模板时只改变已有文字槽位的 content；保留所有 id、位置、样式和视觉组件。 */
export const fillTemplateText = (components, resume) => {
  const sectionMap = new Map(resume.sections.map(item => [canonical(item.title), item.body]))
  const used = new Set()
  let changed = 0
  const filled = components.map(component => {
    const label = normalize(component.label)
    const id = String(component.id || '').toLowerCase()
    let content
    if (component.type === 'contact') {
      const icon = component.style?.icon || ''
      const contact = resume.contacts.find(item => {
        const key = normalize(item.label)
        return label.includes(key) || key.includes(label) || (icon === 'phone' && /电话|手机/.test(key))
          || (icon === 'email' && /邮箱|邮件/.test(key)) || (icon === 'address' && /城市|地址|所在地/.test(key))
          || (icon === 'github' && /github/i.test(key)) || (icon === 'wechat' && /微信/.test(key))
      })
      content = contact?.content || ''
    } else if (['text', 'title', 'experience', 'skills'].includes(component.type)) {
      if (id === 'name' || /^(姓名|姓名标题)$/.test(label)) content = resume.name
      else if (id === 'role' || /^(求职意向|应聘岗位|目标岗位|岗位名称)$/.test(label)) content = resume.title
      else if (component.type !== 'title' && !label.endsWith('标题') && text(component.content) !== text(component.label)) {
        const key = canonical(label.endsWith('内容') ? label.slice(0, -2) : label)
        if (sectionAliases.some(group => group[0] === key)) {
          content = sectionMap.get(key) || ''
          if (content) used.add(key)
        } else if (component.type === 'text' || component.type === 'experience' || component.type === 'skills') {
          // 模板中未能对应用户事实的示例文字不可混进新简历。
          content = ''
        }
      }
    }
    if (content === undefined || content === component.content) return component
    changed += 1
    return { ...component, content }
  })
  return { components: filled, changed, unmatched: resume.sections.filter(item => !used.has(canonical(item.title))).map(item => item.title) }
}

export const createSmartResumeDraft = (resume, current) => {
  const template = current?.templateId && (current.components || []).length
  const mapping = template ? fillTemplateText(current.components, resume) : null
  return {
    title: `${resume.name || '我的'} · ${resume.title || '智能简历'}`,
    targetJob: resume.title || current?.targetJob || '',
    templateId: template ? current.templateId : null,
    draft: true,
    components: template ? mapping.components : layoutStructuredResume(resume),
    style: template ? { ...current.style } : defaultPageStyle(),
    unmatched: mapping?.unmatched || [],
    changed: mapping?.changed || 0
  }
}
