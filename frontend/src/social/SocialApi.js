import httpClient from '../api/httpClient'

export const socialApi = {
  search: (username) => httpClient.get(`/social/search/${encodeURIComponent(username)}`).then((r) => r.data),
  getProfile: (username) => httpClient.get(`/social/profiles/${encodeURIComponent(username)}`).then((r) => r.data),
  updateProfile: (payload) => httpClient.put('/social/profile', payload).then((r) => r.data),
  changePrivacy: (isPrivate) => httpClient.put('/social/profile/privacy', { isPrivate }).then((r) => r.data),
  follow: (userId) => httpClient.post(`/social/users/${userId}/follow`).then((r) => r.data),
  unfollow: (userId) => httpClient.delete(`/social/users/${userId}/follow`).then((r) => r.data),
  removeFollower: (userId) => httpClient.delete(`/social/followers/${userId}`).then((r) => r.data),
  followers: (username, cursor, size = 20) =>
    httpClient.get(`/social/profiles/${encodeURIComponent(username)}/followers`, { params: { cursor, size } }).then((r) => r.data),
  following: (username, cursor, size = 20) =>
    httpClient.get(`/social/profiles/${encodeURIComponent(username)}/following`, { params: { cursor, size } }).then((r) => r.data),
  followRequests: (cursor, size = 20) =>
    httpClient.get('/social/follow-requests', { params: { cursor, size } }).then((r) => r.data),
  approveRequest: (userId) => httpClient.post(`/social/follow-requests/${userId}/approve`).then((r) => r.data),
  rejectRequest: (userId) => httpClient.post(`/social/follow-requests/${userId}/reject`).then((r) => r.data),
  block: (userId) => httpClient.post(`/social/users/${userId}/block`).then((r) => r.data),
  unblock: (userId) => httpClient.delete(`/social/users/${userId}/block`).then((r) => r.data),
  blockedUsers: (cursor, size = 20) =>
    httpClient.get('/social/blocked-users', { params: { cursor, size } }).then((r) => r.data),
}
