/**
 * 用户接口请求
 * 功能：封装登录、注册、登出、当前用户资料接口
 */
import request from './request'

export const login = (data) => request.post('/user/login', data)
export const register = (data) => request.post('/user/register', data)
export const logout = () => request.post('/user/logout')
export const getMe = () => request.get('/user/me', { skipAuthRedirect: true })
export const getProfile = () => request.get('/user/profile')
export const getUserSystemConfig = () => request.get('/user/system-config')

/** 发送注册邮箱验证码 */
export const sendRegisterCode = (email) => request.post('/user/send-code', null, { params: { email } })

/** 查询当前用户今日额度（AI/导出 上限、已用、剩余） */
export const getUserQuota = () => request.get('/user/quota', { skipAuthRedirect: true })

/** 更新当前用户资料（昵称 / 头像 / 邮箱） */
export const updateMyProfile = (data) => request.post('/user/profile', data)

/** 当前用户修改密码 */
export const changeMyPassword = (data) => request.post('/user/change-password', data)

/** 查询当前用户操作记录 */
export const listMyActivities = () => request.get('/user/activities')

/** 清空当前用户操作记录 */
export const clearMyActivities = () => request.delete('/user/activities')

