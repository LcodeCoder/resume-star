/**
 * 简历接口请求
 * 功能：封装简历列表、保存草稿和模板套用接口
 */
import request from './request'

export const listResumes = (params) => request.get('/resumes', { params })
export const saveResume = (data) => request.post('/resumes', data)
export const applyTemplate = (templateId, params) => request.post(`/resumes/apply-template/${templateId}`, null, { params })
