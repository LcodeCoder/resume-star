const snapshotOf = (item) => ({
  type: item.type || 'text',
  label: item.label || '',
  content: item.content || '',
  x: item.x || 0,
  y: item.y || 0,
  width: item.width || 0,
  height: item.height || 0,
  hidden: !!item.hidden,
  src: item.src ? '[已上传图片]' : ''
})

const FIELD_LABEL = {
  type: '类型',
  label: '名称',
  content: '文字',
  x: '横向位置',
  y: '纵向位置',
  width: '宽度',
  height: '高度',
  hidden: '显隐',
  src: '图片'
}

const clip = (value) => {
  const text = String(value ?? '')
  return text.length > 80 ? `${text.slice(0, 80)}…` : text
}

const samePlace = (a, b) =>
  a.type === b.type && a.label === b.label && Math.abs((a.x || 0) - (b.x || 0)) < 8 && Math.abs((a.y || 0) - (b.y || 0)) < 8

/**
 * 对比两份组件树。优先按 id，对不上的再按类型+名称+位置兜底。
 */
export const diffResumeComponents = (current = [], previous = []) => {
  const curr = [...(current || [])]
  const prev = [...(previous || [])]
  const prevById = new Map(prev.map((item) => [item.id, item]))
  const usedPrev = new Set()
  const added = []
  const removed = []
  const changed = []

  curr.forEach((item) => {
    let before = item.id ? prevById.get(item.id) : null
    if (before) usedPrev.add(before.id)
    else {
      before = prev.find((candidate) => !usedPrev.has(candidate.id) && samePlace(candidate, item))
      if (before) usedPrev.add(before.id)
    }
    if (!before) {
      added.push({ id: item.id, label: item.label || item.type, content: item.content || '' })
      return
    }
    const nextSnap = snapshotOf(item)
    const prevSnap = snapshotOf(before)
    const fields = Object.keys(nextSnap)
      .filter((key) => String(nextSnap[key]) !== String(prevSnap[key]))
      .map((key) => ({
        field: FIELD_LABEL[key] || key,
        before: clip(prevSnap[key]),
        after: clip(nextSnap[key])
      }))
    if (fields.length) {
      changed.push({ id: item.id, label: item.label || item.type, fields })
    }
  })

  prev.forEach((item) => {
    if (!usedPrev.has(item.id)) {
      removed.push({ id: item.id, label: item.label || item.type, content: item.content || '' })
    }
  })

  return { added, removed, changed, total: added.length + removed.length + changed.length }
}
