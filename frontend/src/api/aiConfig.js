import request from './request'

/**
 * AI 配置管理接口
 * 功能：后台管理员配置 AI 接口地址、API Key、模型参数
 */

/**
 * 查询所有 AI 配置列表
 */
export const listAiConfigs = () => request.get('/admin/ai-configs')

/**
 * 保存或更新 AI 配置
 */
export const saveAiConfig = (data) => request.post('/admin/ai-configs', data)

/**
 * 删除 AI 配置
 */
export const deleteAiConfig = (id) => request.delete(`/admin/ai-configs/${id}`)

/**
 * 启用指定 AI 配置
 */
export const enableAiConfig = (id) => request.post(`/admin/ai-configs/${id}/enable`)

/** 测试连通性：可传当前表单或某条配置；管理员可见状态与错误信息（不显示内部推理） */
export const testAiConfig = (data = {}) => request.post('/admin/ai-configs/test', data, { timeout: 400000, silentError: true })
