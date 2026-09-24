import request from './request'

export const getSmartResumeQuota = () => request.get('/smart-resume/quota', { silentError: true })
export const generateSmartResume = input => request.post('/smart-resume/generate', input, {
  silentError: true
})
export const getSmartResumeJob = id => request.get(`/smart-resume/jobs/${encodeURIComponent(id)}`, {
  silent: true, silentError: true
})
export const getLatestSmartResumeJob = async () => {
  const job = await request.get('/smart-resume/jobs/latest', { silent: true, silentError: true })
  // 通用响应解包在 data=null 时会保留外层 Result；首次使用尚无任务。
  return job?.id ? job : null
}
