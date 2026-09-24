import request from './request'

export const loadCareerWorkspace = () => request.get('/career-lab/workspace')
export const saveCareerWorkspace = (workspace) => request.put('/career-lab/workspace', workspace, { silent: true })
export const askCareerCoach = (mode, context) => request.post('/career-lab/assist', { mode, context }, { timeout: 400000, silentError: true })

export const appendCareerPractice = (practice) => request.post('/career-lab/practice', practice, { silent: true })

/** 仅提交 JD；后端从登录用户的工作区读取经历并在 Qdrant 中更新索引。 */
export const analyzeCareerEvidence = (jd) => request.post('/career-lab/evidence/analyze', { jd }, { timeout: 180000, silentError: true })
