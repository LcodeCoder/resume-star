/** 兼容接口可能把模型内部思考混入 content；UI/语音只消费最终正文。 */
export const visibleAiText = (value) => {
  if (typeof value !== 'string') return ''
  return value
    .replace(/<\s*(think|thinking|analysis)\b[^>]*>[\s\S]*?<\s*\/\s*\1\s*>/gi, '')
    .replace(/<\s*(?:think|thinking|analysis)\b[^>]*>[\s\S]*$/i, '')
    .trim()
}
