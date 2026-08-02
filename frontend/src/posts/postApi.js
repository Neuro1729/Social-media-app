import httpClient from '../api/httpClient'

export const postApi = {
  create: (body) => httpClient.post('/posts', { body }).then((r) => r.data),
  get: (id) => httpClient.get(`/posts/${id}`).then((r) => r.data),
  profilePosts: (username, cursor, size = 20) =>
    httpClient
      .get(`/profiles/${encodeURIComponent(username)}/posts`, { params: { cursor, size } })
      .then((r) => r.data),
  remove: (id) => httpClient.delete(`/posts/${id}`).then((r) => r.data),
  like: (id) => httpClient.put(`/posts/${id}/like`).then((r) => r.data),
  unlike: (id) => httpClient.delete(`/posts/${id}/like`).then((r) => r.data),
  likes: (id, cursor, size = 20) =>
    httpClient.get(`/posts/${id}/likes`, { params: { cursor, size } }).then((r) => r.data),
  createComment: (id, body) =>
    httpClient.post(`/posts/${id}/comments`, { body }).then((r) => r.data),
  comments: (id, cursor, size = 20) =>
    httpClient.get(`/posts/${id}/comments`, { params: { cursor, size } }).then((r) => r.data),
  deleteComment: (id) => httpClient.delete(`/comments/${id}`).then((r) => r.data),
}
