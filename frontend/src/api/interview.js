import request from './request'

/**
 * 获取面试分类列表
 */
export function getInterviewCategories() {
  return request.get('/interview/categories')
}

/**
 * 生成面试问题
 */
export function generateInterviewQuestion(data) {
  return request.post('/interview/question', data)
}

/**
 * 提交面试答案并生成报告
 */
export function submitInterview(data) {
  return request.post('/interview/submit', data)
}

/**
 * 获取用户的面试历史列表
 */
export function getInterviewRecords(userId) {
  return request.get('/interview/records', { params: { userId } })
}

/**
 * 获取面试记录详情
 */
export function getInterviewRecordDetail(recordId, userId) {
  return request.get(`/interview/records/${recordId}`, { params: { userId } })
}

/**
 * 删除面试记录
 */
export function deleteInterviewRecord(recordId, userId) {
  return request.delete(`/interview/records/${recordId}`, { params: { userId } })
}

/**
 * 获取用户剩余面试次数
 */
export function getInterviewQuota(userId) {
  return request.get('/interview/quota', { params: { userId } })
}

/**
 * 获取面试公开配置（总时长、最大题数、开场白等）
 */
export function getInterviewConfig() {
  return request.get('/interview/config')
}
