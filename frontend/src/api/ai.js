/**
 * AI 接口请求
 * 功能：所有 AI 能力均通过后端代理调用，前端不保存也不传递 API Key
 */
import request from './request'

export const optimizeResume = (data) => request.post('/ai/optimize', data)
