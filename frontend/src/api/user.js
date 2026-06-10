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

