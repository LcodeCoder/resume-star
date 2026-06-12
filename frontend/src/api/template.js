/**
 * 模板接口请求
 * 功能：封装模板分类、模板列表、模板详情和 VIP 权限配置接口
 */
import request from './request'

export const listTemplateCategories = () => request.get('/templates/categories')
export const listTemplates = (params) => request.get('/templates', { params })
export const getTemplate = (templateId) => request.get(`/templates/${templateId}`)
export const getTemplateVipConfig = () => request.get('/templates/vip-config')

/** 查询我收藏的模板列表 */
export const listFavoriteTemplates = (params) => request.get('/templates/favorites', { params })

/** 收藏 / 取消收藏模板（切换），返回 true-已收藏 false-已取消 */
export const toggleTemplateFavorite = (templateId, params) => request.post(`/templates/${templateId}/favorite`, null, { params })

/** 增加模板浏览量 */
export const incrementTemplateView = (templateId) => request.post(`/templates/${templateId}/view`)
