/**
 * 模板接口请求
 * 功能：封装模板分类、模板列表、模板详情和 VIP 权限配置接口
 */
import request from './request'

export const listTemplateCategories = () => request.get('/templates/categories')
export const listTemplates = (params) => request.get('/templates', { params })
export const getTemplate = (templateId) => request.get(`/templates/${templateId}`)
export const getTemplateVipConfig = () => request.get('/templates/vip-config')
