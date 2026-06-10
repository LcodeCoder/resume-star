/**
 * 管理员认证接口
 * 功能：管理员登录、登出、当前管理员
 */
import request from './request'

export const adminLogin = (data) => request.post('/admin/auth/login', data)
export const adminLogout = () => request.post('/admin/auth/logout')
export const getAdminMe = () => request.get('/admin/auth/me', { skipAuthRedirect: true })
