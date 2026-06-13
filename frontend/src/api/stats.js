/**
 * 站点统计接口请求
 * 功能：获取首页展示的公开平台统计（模板数、行业分类数、累计 AI 优化次数）
 */
import request from './request'

export const getSiteStats = () => request.get('/stats/site')
