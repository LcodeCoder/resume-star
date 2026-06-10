/**
 * Axios 请求封装
 * 功能：统一配置后端 API 地址、Cookie 凭据、响应解包和错误提示
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true
})

service.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result && result.success === false) {
      if (!response.config?.skipAuthRedirect) {
        ElMessage.error(result.message || '请求失败')
      }
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    return result?.data ?? result
  },
  (error) => {
    // 401 未登录：静默恢复登录态的请求不触发跳转，避免管理员页被用户态检查误伤
    if (error.response?.status === 401) {
      if (error.config?.skipAuthRedirect) {
        return Promise.reject(error)
      }
      const url = error.config?.url || ''
      const isAdmin = url.startsWith('/admin')
      const target = isAdmin ? '/admin/login' : '/login'
      if (window.location.pathname !== target) {
        window.location.href = target
      }
      return Promise.reject(error)
    }
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default service

