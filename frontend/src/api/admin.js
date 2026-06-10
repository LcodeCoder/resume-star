/**
 * 后台接口请求
 * 功能：查询统计概览，管理模板（创建、删除），会员配置接口预留
 */
import request from './request'

export const getDashboard = () => request.get('/admin/dashboard')

/**
 * 创建简历模板
 * @param data 模板基础信息：name 名称、categoryCode 分类、styleTag 风格标签、
 *             accentColor 主题色、variant 版式（classic/banner/minimal）、vipTemplate 会员标识
 */
export const createTemplate = (data) => request.post('/admin/templates', data)

/** 删除指定模板 */
export const deleteTemplate = (templateId) => request.delete(`/admin/templates/${templateId}`)
