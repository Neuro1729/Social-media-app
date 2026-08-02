import httpClient from '../api/httpClient'

export const accountApi = {
  me: () => httpClient.get('/account/me').then((r) => r.data),
  sessions: () => httpClient.get('/account/sessions').then((r) => r.data),
  revokeSession: (sessionId) => httpClient.delete(`/account/sessions/${sessionId}`).then((r) => r.data),
  revokeAllSessions: () => httpClient.delete('/account/sessions').then((r) => r.data),
  changeUsername: (username) => httpClient.put('/account/username', { username }).then((r) => r.data),
  removeUsername: () => httpClient.delete('/account/username').then((r) => r.data),
}
