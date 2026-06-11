/**
 * 简历接口请求
 * 功能：封装简历列表、保存草稿和模板套用接口
 */
import request from './request'

export const listResumes = (params) => request.get('/resumes', { params })
export const listDraftResumes = (params) => request.get('/resumes/drafts', { params })
export const listPublishedResumes = (params) => request.get('/resumes/published', { params })
export const saveResume = (data) => request.post('/resumes', data)
export const applyTemplate = (templateId, params) => request.post(`/resumes/apply-template/${templateId}`, null, { params })

/** 发布草稿（草稿转正式简历） */
export const publishResume = (resumeId, params) => request.put(`/resumes/${resumeId}/publish`, null, { params })

/** 删除简历 */
export const deleteResume = (resumeId, params) => request.delete(`/resumes/${resumeId}`, { params })

/** 新建空白简历 */
export const createBlankResume = (params) => request.post('/resumes/blank', null, { params })

/** 复制简历 */
export const copyResume = (resumeId, params) => request.post(`/resumes/${resumeId}/copy`, null, { params })

/** 查询简历历史版本 */
export const listResumeVersions = (resumeId) => request.get(`/resumes/${resumeId}/versions`)

/** 回滚到指定历史版本 */
export const restoreResumeVersion = (resumeId, versionId) => request.post(`/resumes/${resumeId}/versions/${versionId}/restore`)

/** 生成 / 获取分享链接 */
export const createResumeShare = (resumeId) => request.post(`/resumes/${resumeId}/share`)

/** 查询当前分享信息 */
export const getResumeShare = (resumeId) => request.get(`/resumes/${resumeId}/share`)

/** 公开访问：按 token 查看分享内容 */
export const viewResumeShare = (token) => request.get(`/resumes/share/${token}`, { skipAuthRedirect: true })
