/**
 * AI 接口请求
 * 功能：所有 AI 能力均通过后端代理调用，前端不保存也不传递 API Key
 */
import request from './request'

/** 后端最多重试 5 次，这里把超时放到 2 分钟，避免前端先断 */
export const optimizeResume = (data) => request.post('/ai/optimize', data, { timeout: 120000, silentError: true })
