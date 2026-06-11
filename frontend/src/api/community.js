/**
 * 社区 API
 */
import request from './request'

export const submitCase = (data) => request.post('/community/cases', data)
export const submitArticle = (data) => request.post('/community/articles', data)
export const approveCase = (id) => request.put(`/community/cases/${id}/approve`)
export const approveArticle = (id) => request.put(`/community/articles/${id}/approve`)
export const deleteCaseByResume = (resumeId) => request.delete(`/community/cases/by-resume/${resumeId}`)
