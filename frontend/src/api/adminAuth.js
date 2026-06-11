/**
 * 管理员认证接口
 * 功能：管理员登录、登出、当前管理员
 */
import request from './request'

export const adminLogin = (data) => request.post('/admin/auth/login', data)
export const adminLogout = () => request.post('/admin/auth/logout')
export const getAdminMe = () => request.get('/admin/auth/me', { skipAuthRedirect: true })

/** 管理员自助修改账号 / 昵称 / 密码 */
export const updateAdminProfile = (data) => request.post('/admin/auth/update-profile', data)
