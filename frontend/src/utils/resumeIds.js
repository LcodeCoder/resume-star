/** 生成画布组件 ID，避免同一毫秒批量创建时撞号 */
export const newComponentId = (prefix = 'c') =>
  `${prefix}${Date.now().toString(36)}${Math.random().toString(36).slice(2, 7)}`
