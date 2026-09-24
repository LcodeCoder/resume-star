import request from './request'

export const getSmartResumeQuota = () => request.get('/smart-resume/quota', { silentError: true })
export const generateSmartResume = input => request.post('/smart-resume/generate', input, {
  silentError: true,
  timeout: 200000
})
