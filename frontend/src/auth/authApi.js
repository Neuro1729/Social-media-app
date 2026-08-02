import httpClient from '../api/httpClient'

export const authApi = {
  register: (payload) => httpClient.post('/auth/register', payload).then((r) => r.data),
  login: (payload) => httpClient.post('/auth/login', payload).then((r) => r.data),
  logout: () => httpClient.post('/auth/logout').then((r) => r.data),
  forgotPassword: (payload) => httpClient.post('/auth/forgot-password', payload).then((r) => r.data),
  resetPassword: (payload) => httpClient.post('/auth/reset-password', payload).then((r) => r.data),
  refresh: () => httpClient.post('/auth/refresh').then((r) => r.data),
}
