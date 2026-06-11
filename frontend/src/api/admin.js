/**
 * 后台接口请求
 * 功能：查询统计概览，管理模板、用户、VIP 权限配置
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

/** 切换模板会员权限 */
export const updateTemplateVip = (templateId, data) => request.patch(`/admin/templates/${templateId}/vip`, data)

/** 查询后台用户列表 */
export const listAdminUsers = () => request.get('/admin/users')

/** 更新用户会员等级 */
export const updateUserVip = (userId, data) => request.post(`/admin/users/${userId}/vip`, data)

/** 重置用户密码 */
export const resetUserPassword = (userId, data) => request.post(`/admin/users/${userId}/reset-password`, data)

/** 删除用户 */
export const deleteAdminUser = (userId) => request.delete(`/admin/users/${userId}`)

/** 查询 VIP 权限配置 */
export const getVipConfig = () => request.get('/admin/vip-config')

/** 设置组件分组会员权限 */
export const updateComponentVip = (data) => request.post('/admin/vip-config/components', data)
