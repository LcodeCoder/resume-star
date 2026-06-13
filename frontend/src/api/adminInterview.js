import request from './request'

/**
 * 获取所有面试分类（管理员）
 */
export function getAdminInterviewCategories() {
  return request.get('/admin/interview-categories')
}

/**
 * 创建面试分类
 */
export function createInterviewCategory(data) {
  return request.post('/admin/interview-categories', data)
}

/**
 * 更新面试分类
 */
export function updateInterviewCategory(id, data) {
  return request.put(`/admin/interview-categories/${id}`, data)
}

/**
 * 删除面试分类
 */
export function deleteInterviewCategory(id) {
  return request.delete(`/admin/interview-categories/${id}`)
}
