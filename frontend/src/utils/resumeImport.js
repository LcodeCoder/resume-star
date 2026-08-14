import { layoutStructuredResume } from './resumeLayout'

const SECTION_ALIASES = [
  ['教育经历', /教育|学历|education/i],
  ['工作经历', /工作经历|实习|任职|employment|experience/i],
  ['项目经历', /项目|project/i],
  ['专业技能', /技能|skill|技术栈/i],
  ['自我评价', /自我评价|个人总结|简介|about|summary/i],
  ['获奖证书', /证书|获奖|honor|award/i],
  ['校园经历', /校园|社团|组织/i]
]

const EMAIL_RE = /[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i
const PHONE_RE = /(?:\+?86[-\s]?)?1[3-9]\d{9}|\d{3,4}[-\s]?\d{7,8}/
const URL_RE = /(?:https?:\/\/|github\.com\/|gitee\.com\/)[^\s]+/i

const normalizeLines = (raw) => String(raw || '')
  .replace(/\r/g, '\n')
  .split('\n')
  .map((line) => line.replace(/\t/g, ' ').replace(/\s{2,}/g, ' ').trim())
  .filter(Boolean)

const matchSection = (line) => {
  const compact = line.replace(/\s+/g, '')
  if (compact.length > 16) return null
  return SECTION_ALIASES.find(([, pattern]) => pattern.test(line) || pattern.test(compact))?.[0] || null
}

const guessContactIcon = (text) => {
  if (EMAIL_RE.test(text)) return 'email'
  if (PHONE_RE.test(text)) return 'phone'
  if (/github/i.test(text)) return 'github'
  if (/微信|wechat/i.test(text)) return 'wechat'
  if (URL_RE.test(text)) return 'website'
  return 'address'
}

export const parseResumeText = (raw, filename = '') => {
  const lines = normalizeLines(raw)
  if (!lines.length) {
    return { name: filename.replace(/\.[^.]+$/, '') || '未命名', title: '求职意向', contacts: [], sections: [] }
  }

  const contacts = []
  const used = new Set()
  lines.forEach((line, index) => {
    const email = line.match(EMAIL_RE)
    const phone = line.match(PHONE_RE)
    const url = line.match(URL_RE)
    if (email) {
      contacts.push({ icon: 'email', label: '邮箱', content: email[0] })
      used.add(index)
    } else if (phone && line.length < 40) {
      contacts.push({ icon: 'phone', label: '电话', content: phone[0] })
      used.add(index)
    } else if (url) {
      contacts.push({ icon: guessContactIcon(line), label: '链接', content: url[0] })
      used.add(index)
    }
  })

  const leftover = lines.filter((_, index) => !used.has(index))
  const name = leftover[0] || filename.replace(/\.[^.]+$/, '') || '姓名'
  let title = leftover[1] && leftover[1].length <= 40 ? leftover[1] : '求职意向'
  const startIndex = leftover[1] && leftover[1].length <= 40 ? 2 : 1

  const sections = []
  let current = { title: '正文', body: [] }
  leftover.slice(startIndex).forEach((line) => {
    const heading = matchSection(line)
    if (heading) {
      if (current.body.length) sections.push({ title: current.title, body: current.body.join('\n') })
      current = { title: heading, body: [] }
      return
    }
    current.body.push(line)
  })
  if (current.body.length) sections.push({ title: current.title, body: current.body.join('\n') })
  if (!sections.length) {
    sections.push({ title: '正文', body: leftover.slice(startIndex).join('\n') })
  }

  return {
    name,
    title,
    contacts: contacts.slice(0, 6),
    sections
  }
}

export const componentsFromResumeText = (raw, filename = '') =>
  layoutStructuredResume(parseResumeText(raw, filename))

const readAsArrayBuffer = (file) => file.arrayBuffer()

const readAsText = (file) => file.text()

export const extractTextFromFile = async (file) => {
  const name = file?.name || '导入简历'
  const type = (file?.type || '').toLowerCase()
  const lower = name.toLowerCase()

  if (lower.endsWith('.txt') || type.startsWith('text/')) {
    return { text: await readAsText(file), filename: name }
  }

  if (lower.endsWith('.docx') || type.includes('wordprocessingml')) {
    const mammoth = await import('mammoth')
    const result = await mammoth.extractRawText({ arrayBuffer: await readAsArrayBuffer(file) })
    return { text: result.value || '', filename: name }
  }

  if (lower.endsWith('.doc')) {
    throw new Error('暂不支持旧版 .doc，请另存为 .docx 或导出 PDF 后再导入')
  }

  if (lower.endsWith('.pdf') || type === 'application/pdf') {
    const pdfjs = await import('pdfjs-dist')
    const worker = await import('pdfjs-dist/build/pdf.worker.min.mjs?url')
    pdfjs.GlobalWorkerOptions.workerSrc = worker.default
    const pdf = await pdfjs.getDocument({ data: await readAsArrayBuffer(file) }).promise
    const pages = []
    for (let i = 1; i <= pdf.numPages; i += 1) {
      const page = await pdf.getPage(i)
      const content = await page.getTextContent()
      const line = content.items.map((item) => item.str).join(' ')
      pages.push(line)
    }
    return { text: pages.join('\n'), filename: name }
  }

  throw new Error('请上传 PDF、Word（.docx）或 TXT 文件')
}

export const importResumeFile = async (file) => {
  const { text, filename } = await extractTextFromFile(file)
  if (!String(text).trim()) throw new Error('没有从文件里读到文字，扫描件请先转成可选中文字的 PDF')
  return {
    title: filename.replace(/\.[^.]+$/, '') || '导入的简历',
    components: componentsFromResumeText(text, filename)
  }
}
