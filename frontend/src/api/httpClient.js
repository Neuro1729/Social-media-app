import axios from 'axios'

let accessTokenGetter = () => null
let accessTokenSetter = () => {}
let onAuthFailure = () => {}

export function bindAuthHandlers({ getToken, setToken, onFailure }) {
  accessTokenGetter = getToken
  accessTokenSetter = setToken
  onAuthFailure = onFailure
}

const httpClient = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

httpClient.interceptors.request.use((config) => {
  const token = accessTokenGetter()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshPromise = null

httpClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    if (!original || original._retry || original.url?.includes('/auth/refresh')) {
      return Promise.reject(error)
    }
    if (error.response?.status !== 401) {
      return Promise.reject(error)
    }
    original._retry = true
    try {
      if (!refreshPromise) {
        refreshPromise = axios
          .post('/api/auth/refresh', {}, { withCredentials: true })
          .then((res) => {
            accessTokenSetter(res.data.accessToken)
            return res.data.accessToken
          })
          .finally(() => {
            refreshPromise = null
          })
      }
      const token = await refreshPromise
      original.headers.Authorization = `Bearer ${token}`
      return httpClient(original)
    } catch (refreshError) {
      onAuthFailure()
      return Promise.reject(refreshError)
    }
  },
)

export default httpClient
