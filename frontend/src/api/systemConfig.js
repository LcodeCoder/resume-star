/**
 * 系统配置接口请求
 * 功能：管理员配置邮箱验证、单IP登录限制、每日导出数量等系统参数
 */
import request from './request'

/** 获取系统配置 */
export const getSystemConfig = () => request.get('/admin/system-config')

/** 更新系统配置 */
export const updateSystemConfig = (data) => request.put('/admin/system-config', data)
