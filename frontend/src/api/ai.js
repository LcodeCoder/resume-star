/**
 * AI 接口请求
 * 功能：所有 AI 能力均通过后端代理调用，前端不保存也不传递 API Key
 */
import request from './request'

/** 推理模型可能需要更长时间生成正文，允许足够完成两轮受限重试，避免前端先断 */
export const optimizeResume = (data) => request.post('/ai/optimize', data, { timeout: 400000, silentError: true })
