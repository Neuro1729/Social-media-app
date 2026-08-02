import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from './AuthProvider'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [loginValue, setLoginValue] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await login({ login: loginValue, password })
      navigate('/account')
    } catch (err) {
      setError(err.response?.data?.error || 'Login failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page">
      <div className="shell">
        <p className="brand">Canopy</p>
        <h1>Welcome back.</h1>
        <p className="lead">Sign in with username, email, or phone.</p>
        <form onSubmit={onSubmit} className="stack">
          <label>
            Username / Email / Phone
            <input value={loginValue} onChange={(e) => setLoginValue(e.target.value)} required autoComplete="username" />
          </label>
          <label>
            Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" />
          </label>
          {error && <p className="error">{error}</p>}
          <button type="submit" disabled={busy}>{busy ? 'Signing in…' : 'Sign In'}</button>
        </form>
        <div className="auth-links">
          <Link to="/forgot-password">Forgot Password</Link>
          <Link to="/register">Create Account</Link>
        </div>
      </div>
    </div>
  )
}
