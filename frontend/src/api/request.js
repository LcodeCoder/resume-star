/**
 * Axios 请求封装
 * 功能：统一配置后端 API 地址、Cookie 凭据、响应解包和错误提示
 *       并把每个请求接入全局加载状态（顶部进度条），可通过 config.silent=true 跳过
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { startLoading, doneLoading } from '../utils/globalLoader'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: Number(import.meta.env.VITE_API_TIMEOUT || 15000),
  withCredentials: true
})

service.interceptors.request.use(
  (config) => {
    // 静默接口（轮询、自动刷新等）可通过 silent: true 跳过进度条，避免一直闪
    if (!config.silent) {
      startLoading()
      config._loadingStarted = true
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    if (response.config?._loadingStarted) doneLoading()
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
    if (error.config?._loadingStarted) doneLoading()
    // 401 未登录：静默恢复登录态的请求不触发跳转。
    // 管理员在「改模板」页也会打到用户接口（自动保存/导出记录等），
    // 那些 401 绝不能把已登录的管理员整页踢去用户登录页。
    if (error.response?.status === 401) {
      if (error.config?.skipAuthRedirect) {
        return Promise.reject(error)
      }
      const url = String(error.config?.url || '')
      const path = window.location.pathname
      const adminApi = url.includes('/admin')
      const adminWorkspace = path.startsWith('/admin')
        || new URLSearchParams(window.location.search).get('adminMode') === 'true'
      if (adminApi) {
        if (path !== '/login') {
          window.location.href = '/login?role=admin'
        }
        return Promise.reject(error)
      }
      if (adminWorkspace) {
        return Promise.reject(error)
      }
      if (path !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }
    if (!error.config?.silentError) ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default service
