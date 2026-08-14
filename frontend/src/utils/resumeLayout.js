import { newComponentId } from './resumeIds'

const PAGE_WIDTH = 794
const MARGIN = 48
const CONTENT_W = PAGE_WIDTH - MARGIN * 2

const estimateHeight = (text, width, fontSize = 14, lineHeight = 1.65) => {
  const charsPerLine = Math.max(8, Math.floor(width / (fontSize * 0.92)))
  const lines = String(text || '').split('\n').reduce(
    (sum, line) => sum + Math.max(1, Math.ceil([...line].length / charsPerLine)),
    0
  )
  return Math.max(Math.round(fontSize * lineHeight) + 6, Math.ceil(lines * fontSize * lineHeight) + 8)
}

const text = (partial) => ({
  type: 'text',
  vipOnly: false,
  ...partial,
  id: partial.id || newComponentId(),
  style: { fontSize: 14, fontWeight: 400, color: '#424245', textAlign: 'left', lineHeight: 1.65, ...(partial.style || {}) }
})

/**
 * 把结构化简历铺成一页可编辑组件（左对齐经典版式）。
 * @param {{ name?: string, title?: string, contacts?: {icon?: string, content: string}[], sections?: { title: string, body: string }[] }} data
 */
export const layoutStructuredResume = (data = {}) => {
  const name = (data.name || '姓名').trim()
  const job = (data.title || '求职意向').trim()
  const contacts = (data.contacts || []).filter((item) => item?.content)
  const sections = (data.sections || []).filter((item) => item?.title || item?.body)
  const components = []
  let y = 44

  components.push(text({
    label: '姓名',
    content: name,
    x: MARGIN,
    y,
    width: CONTENT_W,
    height: 52,
    style: { fontSize: 32, fontWeight: 800, color: '#1d1d1f', letterSpacing: '0.04em' }
  }))
  y += 54

  components.push(text({
    label: '求职意向',
    content: job,
    x: MARGIN,
    y,
    width: CONTENT_W,
    height: 28,
    style: { fontSize: 15, fontWeight: 500, color: '#6e6e73' }
  }))
  y += 36

  contacts.forEach((item, index) => {
    components.push({
      id: newComponentId('ct'),
      type: 'contact',
      label: item.label || '联系方式',
      content: item.content,
      x: MARGIN + (index % 2) * 350,
      y,
      width: 330,
      height: 28,
      style: { fontSize: 13, color: '#424245', icon: item.icon || 'phone' }
    })
    if (index % 2 === 1) y += 30
  })
  if (contacts.length % 2 === 1) y += 30
  y += 10

  components.push({
    id: newComponentId('dv'),
    type: 'divider',
    label: '分割线',
    content: '',
    x: MARGIN,
    y,
    width: CONTENT_W,
    height: 8,
    style: { borderColor: '#d2d2d7', lineWidth: 1 }
  })
  y += 22

  sections.forEach((section) => {
    const heading = (section.title || '章节').trim()
    const body = (section.body || '').trim()
    components.push(text({
      label: heading,
      content: heading,
      x: MARGIN,
      y,
      width: CONTENT_W,
      height: 34,
      style: { fontSize: 18, fontWeight: 700, color: '#1d1d1f' }
    }))
    y += 38
    if (body) {
      const height = estimateHeight(body, CONTENT_W, 14, 1.7)
      components.push(text({
        label: `${heading}内容`,
        content: body,
        x: MARGIN,
        y,
        width: CONTENT_W,
        height,
        style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.7 }
      }))
      y += height + 18
    }
  })

  return components
}

export const defaultPageStyle = () => ({
  background: '#ffffff',
  padding: 40,
  lineHeight: 1.6,
  fontSize: 14
})
