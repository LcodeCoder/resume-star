import test from 'node:test'
import assert from 'node:assert/strict'
import { validateGeneratedResume, fillTemplateText, createSmartResumeDraft } from '../src/utils/smartResume.js'

test('template changes content only, clears old sample claims and keeps visual elements', () => {
  const template = [
    { id: 'name', type: 'title', label: '姓名标题', content: '张三', x: 12, style: { color: '#fff' } },
    { id: 'contact-email', type: 'contact', label: '邮箱地址', content: 'fake@example.com', style: { icon: 'email' } },
    { id: 'project', type: 'text', label: '项目经历', content: '原模板虚构的项目', x: 42, style: { fontSize: 12 } },
    { id: 'skills', type: 'text', label: '专业技能', content: '旧技能', x: 42 },
    { id: 'project-title', type: 'title', label: '项目经历标题', content: '项目经历', x: 42 },
    { id: 'avatar', type: 'image', label: '照片', src: 'data:image/png;base64,abc', x: 88 }
  ]
  const resume = validateGeneratedResume({ name: '小李', title: '前端工程师', sections: [{ title: '专业技能', body: 'Vue 3' }] })
  const result = fillTemplateText(template, resume)
  assert.equal(result.components[0].content, '小李')
  assert.equal(result.components[1].content, '')
  assert.equal(result.components[2].content, '')
  assert.equal(result.components[3].content, 'Vue 3')
  assert.deepEqual(result.components[4], template[4])
  assert.deepEqual(result.components[5], template[5])
  assert.deepEqual(result.components.map(({ id, x, style }) => ({ id, x, style })), template.map(({ id, x, style }) => ({ id, x, style })))
  assert.equal(createSmartResumeDraft(resume, { templateId: 9, components: template, style: { background: '#fefefe' } }).templateId, 9)
})

test('no template creates editable content and invalid response is rejected', () => {
  const resume = validateGeneratedResume({ name: '小李', sections: [{ title: '项目经历', body: '实现用户认证' }] })
  const draft = createSmartResumeDraft(resume, { templateId: null, components: [] })
  assert.equal(draft.templateId, null)
  assert.ok(draft.components.some(component => component.content === '实现用户认证'))
  assert.throws(() => validateGeneratedResume({ sections: [] }), /简历结构|经历/)
})

test('section components are populated, headings stay headings and unsupported examples are cleared', () => {
  const source = [
    { id: 'heading', type: 'text', label: '项目经历', content: '项目经历', style: { fontSize: 19 } },
    { id: 'project-body', type: 'experience', label: '项目经历', content: '某公司 10 倍提升', style: { fontSize: 12 } },
    { id: 'skills-body', type: 'skills', label: '技能特长', content: '旧技术栈' },
    { id: 'award-body', type: 'experience', label: '获奖经历', content: '示例奖学金' },
    { id: 'misc', type: 'text', label: '量化成果', content: '用户增长 200%' }
  ]
  const result = fillTemplateText(source, validateGeneratedResume({ sections: [
    { title: '项目经历', body: '实现后台权限' }, { title: '专业技能', body: 'Java / MySQL' }
  ] }))
  assert.deepEqual(result.components.map(item => item.content), [
    '项目经历', '实现后台权限', 'Java / MySQL', '', ''
  ])
  assert.deepEqual(result.components.map(item => item.style), source.map(item => item.style))
})
