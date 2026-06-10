/**
 * 导出接口请求
 * 功能：记录 PDF / 图片导出行为，并接收会员额度预留字段
 */
import request from './request'

export const recordExport = (data) => request.post('/export/record', data)
