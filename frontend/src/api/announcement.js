/**
 * 站内公告接口请求（用户端）
 * 功能：获取当前启用的最新一条公告，用于进站弹窗展示
 */
import request from './request'

/** 获取当前启用的公告，无则返回 null（静默，不触发未登录跳转） */
export const getActiveAnnouncement = () => request.get('/templates/announcement', { skipAuthRedirect: true })
