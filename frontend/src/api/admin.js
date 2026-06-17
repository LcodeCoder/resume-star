/**
 * 后台接口请求
 * 功能：查询统计概览，管理模板、用户、VIP 权限配置
 */
import request from './request'

export const getDashboard = () => request.get('/admin/dashboard')

/** 查询营收概览 */
export const getRevenue = () => request.get('/admin/revenue')

/** 查询后台操作审计日志 */
export const listAuditLogs = () => request.get('/admin/audit-logs')

/** 清空审计日志 */
export const clearAuditLogs = () => request.delete('/admin/audit-logs')

/**
 * 创建简历模板
 * @param data 模板基础信息：name 名称、categoryCode 分类、styleTag 风格标签、
 *             accentColor 主题色、variant 版式（classic/banner/minimal）、vipTemplate 会员标识
 */
export const createTemplate = (data) => request.post('/admin/templates', data)

/** 删除指定模板 */
export const deleteTemplate = (templateId) => request.delete(`/admin/templates/${templateId}`)

/* ===== 模板分类管理 ===== */

/** 创建模板分类 */
export const createTemplateCategory = (data) => request.post('/admin/template-categories', data)

/** 更新模板分类 */
export const updateTemplateCategory = (id, data) => request.put(`/admin/template-categories/${id}`, data)

/** 删除模板分类 */
export const deleteTemplateCategory = (id) => request.delete(`/admin/template-categories/${id}`)

/* ===== 模板管理 ===== */


/** 更新模板内容 */
export const updateTemplate = (templateId, data) => request.put(`/admin/templates/${templateId}`, data)

/** 分页查询后台模板列表（params: { page, size, categoryCode, keyword }），返回 { records, total, page, size } */
export const listAdminTemplates = (params = {}) => request.get('/admin/templates', { params })

/** 切换模板会员权限 */
export const updateTemplateVip = (templateId, data) => request.patch(`/admin/templates/${templateId}/vip`, data)

/** 分页查询后台用户列表（params: { page, size, keyword }），返回 { records, total, page, size } */
export const listAdminUsers = (params = {}) => request.get('/admin/users', { params })

/** 更新用户会员等级 */
export const updateUserVip = (userId, data) => request.post(`/admin/users/${userId}/vip`, data)

/** 重置用户密码 */
export const resetUserPassword = (userId, data) => request.post(`/admin/users/${userId}/reset-password`, data)

/** 删除用户 */
export const deleteAdminUser = (userId) => request.delete(`/admin/users/${userId}`)

/** 封禁 / 解封用户 */
export const setUserBanned = (userId, data) => request.post(`/admin/users/${userId}/ban`, data)

/* ===== 会员套餐管理 ===== */

/** 查询会员套餐列表 */
export const listMemberPackages = () => request.get('/admin/member-packages')

/** 新增 / 编辑会员套餐（含 id 则编辑） */
export const saveMemberPackage = (data) => request.post('/admin/member-packages', data)

/** 删除会员套餐 */
export const deleteMemberPackage = (packageId) => request.delete(`/admin/member-packages/${packageId}`)

/* ===== 会员兑换码管理 ===== */

/** 分页查询兑换码列表（params: { page, size }），返回 { records, total, page, size } */
export const listRedeemCodes = (params = {}) => request.get('/admin/redeem-codes', { params })

/** 批量生成兑换码 */
export const generateRedeemCodes = (data) => request.post('/admin/redeem-codes', data)

/** 删除兑换码 */
export const deleteRedeemCode = (id) => request.delete(`/admin/redeem-codes/${id}`)

/* ===== 额度套餐 / 额度兑换码管理 ===== */

/** 查询额度套餐列表 */
export const listQuotaPackages = () => request.get('/admin/quota-packages')

/** 新增 / 编辑额度套餐（含 id 则编辑） */
export const saveQuotaPackage = (data) => request.post('/admin/quota-packages', data)

/** 删除额度套餐 */
export const deleteQuotaPackage = (packageId) => request.delete(`/admin/quota-packages/${packageId}`)

/** 分页查询额度兑换码列表（params: { page, size }），返回 { records, total, page, size } */
export const listQuotaCodes = (params = {}) => request.get('/admin/quota-codes', { params })

/** 批量生成额度兑换码 */
export const generateQuotaCodes = (data) => request.post('/admin/quota-codes', data)

/** 删除额度兑换码 */
export const deleteQuotaCode = (id) => request.delete(`/admin/quota-codes/${id}`)

/* ===== AI 调用日志 / 导出记录（纯 DB，后端分页） ===== */

/** 分页查询 AI 调用日志（params: { page, size, userId? }），返回 { records, total, page, size } */
export const listAiCallLogs = (params = {}) => request.get('/admin/ai-call-logs', { params })

/** 分页查询导出记录（params: { page, size, userId? }），返回 { records, total, page, size } */
export const listExportRecords = (params = {}) => request.get('/admin/export-records', { params })

/** 查询 VIP 权限配置 */
export const getVipConfig = () => request.get('/admin/vip-config')

/** 设置组件分组会员权限 */
export const updateComponentVip = (data) => request.post('/admin/vip-config/components', data)

/** 设置单个组件会员权限（细粒度，data: { componentKey, vipOnly }） */
export const updateComponentKeyVip = (data) => request.post('/admin/vip-config/component-key', data)

/* ===== 站内公告管理 ===== */

/** 查询公告列表 */
export const listAnnouncements = () => request.get('/admin/announcements')

/** 新增 / 编辑公告（含 id 则编辑） */
export const saveAnnouncement = (data) => request.post('/admin/announcements', data)

/** 删除公告 */
export const deleteAnnouncement = (id) => request.delete(`/admin/announcements/${id}`)
