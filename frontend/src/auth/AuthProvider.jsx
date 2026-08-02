import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { bindAuthHandlers } from '../api/httpClient'
import { authApi } from './authApi'
import { accountApi } from '../account/accountApi'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(null)
  const [user, setUser] = useState(null)
  const [bootstrapping, setBootstrapping] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    bindAuthHandlers({
      getToken: () => accessToken,
      setToken: setAccessToken,
      onFailure: () => {
        setAccessToken(null)
        setUser(null)
        navigate('/login')
      },
    })
  }, [accessToken, navigate])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        const data = await authApi.refresh()
        if (cancelled) return
        setAccessToken(data.accessToken)
        setUser(data.user)
      } catch {
        if (!cancelled) {
          setAccessToken(null)
          setUser(null)
        }
      } finally {
        if (!cancelled) setBootstrapping(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  const value = useMemo(() => ({
    accessToken,
    user,
    bootstrapping,
    async login(payload) {
      const data = await authApi.login({
        ...payload,
        deviceName: navigator.userAgent.slice(0, 80),
      })
      setAccessToken(data.accessToken)
      setUser(data.user)
      return data
    },
    async register(payload) {
      const data = await authApi.register(payload)
      setAccessToken(data.accessToken)
      setUser(data.user)
      return data
    },
    async logout() {
      try {
        await authApi.logout()
      } finally {
        setAccessToken(null)
        setUser(null)
        navigate('/login')
      }
    },
    async refreshUser() {
      const me = await accountApi.me()
      setUser(me)
      return me
    },
    setAccessToken,
    setUser,
  }), [accessToken, user, bootstrapping, navigate])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
