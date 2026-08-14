import { PDFDocument, rgb, StandardFonts } from 'pdf-lib'
import fontkit from '@pdf-lib/fontkit'

const PAGE_PX_W = 794
const PAGE_PX_H = 1123
const A4_W = 595.28
const A4_H = 841.89
const SX = A4_W / PAGE_PX_W
const SY = A4_H / PAGE_PX_H
const FONT_URLS = [
  '/fonts/NotoSansSC-Regular.otf',
  'https://cdn.jsdelivr.net/gh/adobe-fonts/source-han-sans@release/SubsetOTF/CN/SourceHanSansCN-Regular.otf'
]

let fontBytesPromise = null

const visibleOf = (components = []) => (components || []).filter((item) => item && !item.hidden)

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

const parseColor = (value, fallback = '#111827') => {
  const raw = String(value || fallback).trim()
  const hex = raw.startsWith('#') ? raw.slice(1) : ''
  if (hex.length === 3) {
    const [r, g, b] = [...hex].map((ch) => parseInt(ch + ch, 16) / 255)
    return rgb(r, g, b)
  }
  if (hex.length === 6) {
    return rgb(
      parseInt(hex.slice(0, 2), 16) / 255,
      parseInt(hex.slice(2, 4), 16) / 255,
      parseInt(hex.slice(4, 6), 16) / 255
    )
  }
  return rgb(0.07, 0.09, 0.15)
}

const pageCountOf = (components = []) => {
  const bottom = visibleOf(components).reduce((max, item) => Math.max(max, (item.y || 0) + (item.height || 0)), 0)
  return Math.max(1, Math.ceil((bottom + 24) / PAGE_PX_H))
}

const loadFontBytes = async () => {
  if (!fontBytesPromise) {
    fontBytesPromise = (async () => {
      let lastError = null
      for (const url of FONT_URLS) {
        try {
          const response = await fetch(url)
          if (!response.ok) throw new Error(`字体下载失败 ${response.status}`)
          const bytes = await response.arrayBuffer()
          if (bytes.byteLength < 50_000) throw new Error('字体文件不完整')
          return bytes
        } catch (error) {
          lastError = error
        }
      }
      throw lastError || new Error('无法加载中文字体')
    })()
  }
  try {
    return await fontBytesPromise
  } catch (error) {
    fontBytesPromise = null
    throw error
  }
}

const wrapText = (text, font, size, maxWidth) => {
  const lines = []
  String(text || '').split('\n').forEach((paragraph) => {
    if (!paragraph) {
      lines.push('')
      return
    }
    let line = ''
    for (const ch of [...paragraph]) {
      const next = line + ch
      if (line && font.widthOfTextAtSize(next, size) > maxWidth) {
        lines.push(line)
        line = ch
      } else {
        line = next
      }
    }
    if (line) lines.push(line)
  })
  return lines.length ? lines : ['']
}

const drawWrapped = (page, font, text, item, pageOffset, color) => {
  const style = item.style || {}
  const fontPx = Number(style.fontSize || 14)
  const size = Math.max(7, fontPx * SY)
  const maxWidth = Math.max(12, (item.width || 200) * SX)
  const lines = wrapText(text, font, size, maxWidth)
  const lineHeight = size * (Number(style.lineHeight) || 1.55)
  const align = style.textAlign || 'left'
  const left = (item.x || 0) * SX
  const boxTop = (item.y || 0) - pageOffset
  lines.forEach((line, index) => {
    const width = font.widthOfTextAtSize(line, size)
    let x = left
    if (align === 'center') x = left + Math.max(0, (maxWidth - width) / 2)
    if (align === 'right') x = left + Math.max(0, maxWidth - width)
    const y = A4_H - (boxTop + fontPx * 0.88 + index * (lineHeight / SY)) * SY
    if (y < 8 || y > A4_H - 8) return
    page.drawText(line, { x, y, size, font, color })
  })
}

const embedImage = async (pdf, src) => {
  if (!src || typeof src !== 'string') return null
  try {
    if (src.startsWith('data:')) {
      const [meta, data] = src.split(',')
      if (!data) return null
      const binary = atob(data)
      const bytes = new Uint8Array(binary.length)
      for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i)
      if (meta.includes('png')) return pdf.embedPng(bytes)
      if (meta.includes('jpeg') || meta.includes('jpg')) return pdf.embedJpg(bytes)
      return null
    }
    const response = await fetch(src)
    if (!response.ok) return null
    const bytes = new Uint8Array(await response.arrayBuffer())
    if (src.toLowerCase().includes('.png') || (response.headers.get('content-type') || '').includes('png')) {
      return pdf.embedPng(bytes)
    }
    return pdf.embedJpg(bytes)
  } catch {
    return null
  }
}

export const buildTextPdfBytes = async (components = [], pageStyle = {}) => {
  const pdf = await PDFDocument.create()
  pdf.registerFontkit(fontkit)
  const fontBytes = await loadFontBytes()
  let font
  try {
    font = await pdf.embedFont(fontBytes, { subset: true })
  } catch {
    font = await pdf.embedFont(fontBytes)
  }
  const fallback = await pdf.embedFont(StandardFonts.Helvetica)
  const pages = pageCountOf(components)
  const bg = parseColor(pageStyle.background || '#ffffff', '#ffffff')

  for (let index = 0; index < pages; index += 1) {
    const page = pdf.addPage([A4_W, A4_H])
    page.drawRectangle({ x: 0, y: 0, width: A4_W, height: A4_H, color: bg })
    const offset = index * PAGE_PX_H
    const items = visibleOf(components).filter((item) => {
      const top = item.y || 0
      const bottom = top + (item.height || 0)
      return bottom > offset && top < offset + PAGE_PX_H
    })

    for (const item of items) {
      const style = item.style || {}
      const x = (item.x || 0) * SX
      const top = (item.y || 0) - offset
      const w = Math.max(4, (item.width || 20) * SX)
      const h = Math.max(2, (item.height || 12) * SY)
      const y = A4_H - top * SY - h

      if (item.type === 'divider') {
        const color = parseColor(style.borderColor || style.color, '#d2d2d7')
        page.drawLine({
          start: { x, y: y + h / 2 },
          end: { x: x + w, y: y + h / 2 },
          thickness: Math.max(0.6, (style.lineWidth || 1) * SY),
          color
        })
        continue
      }
      if (item.type === 'block') {
        page.drawRectangle({
          x,
          y,
          width: w,
          height: h,
          color: parseColor(style.background, '#f5f5f7')
        })
        continue
      }
      if (['avatar', 'image', 'qrcode'].includes(item.type) && item.src) {
        const image = await embedImage(pdf, item.src)
        if (image) page.drawImage(image, { x, y, width: w, height: h })
        continue
      }
      if (item.type === 'progress') {
        const percent = Math.max(0, Math.min(100, Number(style.percent ?? 60)))
        page.drawRectangle({ x, y, width: w, height: Math.max(4, h * 0.28), color: parseColor('#e8eef5') })
        page.drawRectangle({
          x,
          y,
          width: w * percent / 100,
          height: Math.max(4, h * 0.28),
          color: parseColor(style.color, '#0071e3')
        })
      }
      const text = componentText(item)
      if (!text) continue
      try {
        drawWrapped(page, font, text, item, offset, parseColor(style.color, '#111827'))
      } catch {
        page.drawText(text.replace(/[^\x00-\x7F]/g, '?'), {
          x,
          y: y + h - 12,
          size: 10,
          font: fallback,
          color: parseColor(style.color, '#111827')
        })
      }
    }
  }

  return pdf.save()
}

export const downloadTextPdf = async (components, pageStyle, filename) => {
  const bytes = await buildTextPdfBytes(components, pageStyle)
  const blob = new Blob([bytes], { type: 'application/pdf' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename.endsWith('.pdf') ? filename : `${filename}.pdf`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  setTimeout(() => URL.revokeObjectURL(url), 1200)
}
