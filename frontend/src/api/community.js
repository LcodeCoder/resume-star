/**
 * 社区 API
 */
import request from './request'

export const submitCase = (data) => request.post('/community/cases', data)
export const submitArticle = (data) => request.post('/community/articles', data)
export const approveCase = (id) => request.put(`/community/cases/${id}/approve`)
export const approveArticle = (id) => request.put(`/community/articles/${id}/approve`)
export const deleteCaseByResume = (resumeId) => request.delete(`/community/cases/by-resume/${resumeId}`)

/** 查询当前用户投稿的优化技巧列表（含待审核） */
export const listMyArticles = (userId) => request.get('/community/my-articles', { params: { userId } })
/** 删除一篇优化技巧投稿 */
export const deleteArticle = (id) => request.delete(`/community/articles/${id}`)

/** 后台：全部案例分页（含待审核），返回 { records, total, page, size } */
export const listAllCases = (params = {}) => request.get('/community/admin/cases', { params })
/** 后台：全部技巧文章分页（含待审核），返回 { records, total, page, size } */
export const listAllArticles = (params = {}) => request.get('/community/admin/articles', { params })
