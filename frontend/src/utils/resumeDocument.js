/**
 * 把画布组件收成可投递文档：Word 文字稿。
 */

const esc = (value) => String(value ?? '')
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')

const visibleOf = (components = []) =>
  (components || []).filter((item) => item && !item.hidden)

const sortedOf = (components = []) =>
  [...visibleOf(components)].sort((a, b) => (a.y || 0) - (b.y || 0) || (a.x || 0) - (b.x || 0))

const componentText = (item) => {
  if (!item) return ''
  if (item.type === 'contact') return item.content || ''
  if (item.type === 'progress') return `${item.content || ''} ${item.style?.percent ?? 60}%`.trim()
  if (item.type === 'rating') {
    const stars = item.style?.stars || 0
    const total = item.style?.total || 5
    return `${item.content || ''} ${stars}/${total}`.trim()
  }
  if (item.type === 'divider' || item.type === 'block') return ''
  if (['avatar', 'image', 'qrcode'].includes(item.type)) return item.src ? '' : (item.content || '')
  return item.content || ''
}

export const extractResumeLines = (components = []) =>
  sortedOf(components)
    .map((item) => ({
      id: item.id,
      type: item.type,
      label: item.label || '',
      text: componentText(item).trim()
    }))
    .filter((item) => item.text)

export const buildLinearResumeHtml = (components = [], title = '简历') => {
  const lines = extractResumeLines(components)
  const body = lines.map((line) => {
    const isHeading = line.type === 'text' && (line.label?.includes('标题') || line.label?.includes('姓名') || [...line.text].length <= 12)
    return isHeading ? `<h2>${esc(line.text)}</h2>` : `<p>${esc(line.text).replace(/\n/g, '<br>')}</p>`
  }).join('')
  return `<!DOCTYPE html><html><head><meta charset="utf-8"><title>${esc(title)}</title>
<style>
  body { margin: 24px; font: 14px/1.7 "PingFang SC", "Microsoft YaHei", sans-serif; color: #111; max-width: 720px; }
  h1 { font-size: 22px; margin: 0 0 16px; }
  h2 { font-size: 16px; margin: 20px 0 8px; }
  p { margin: 0 0 8px; white-space: pre-wrap; }
</style></head><body><h1>${esc(title)}</h1>${body || '<p>暂无文字内容</p>'}</body></html>`
}

export const downloadTextFile = (content, filename, mime = 'text/plain;charset=utf-8') => {
  const blob = new Blob([content], { type: mime })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export const downloadLinearWord = (components, title, filename) => {
  const html = buildLinearResumeHtml(components, title)
  downloadTextFile(`\ufeff${html}`, filename, 'application/msword')
}
