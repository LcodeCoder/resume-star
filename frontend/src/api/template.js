/**
 * 模板接口请求
 * 功能：封装模板分类、模板列表、模板详情接口，支持会员模板标识展示
 */
import request from './request'

export const listTemplateCategories = () => request.get('/templates/categories')
export const listTemplates = (params) => request.get('/templates', { params })
export const getTemplate = (templateId) => request.get(`/templates/${templateId}`)
