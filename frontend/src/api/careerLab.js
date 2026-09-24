import request from './request'

export const loadCareerWorkspace = () => request.get('/career-lab/workspace')
export const saveCareerWorkspace = (workspace) => request.put('/career-lab/workspace', workspace, { silent: true })
export const askCareerCoach = (mode, context) => request.post('/career-lab/assist', { mode, context }, { timeout: 400000, silentError: true })

export const appendCareerPractice = (practice) => request.post('/career-lab/practice', practice, { silent: true })
