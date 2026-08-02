import { useState } from 'react'
import { Link } from 'react-router-dom'
import { authApi } from '../auth/authApi'

export default function ForgotPasswordPage() {
  const [login, setLogin] = useState('')
  const [message, setMessage] = useState('')
  const [resetToken, setResetToken] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    setMessage('')
    setResetToken('')
    try {
      const data = await authApi.forgotPassword({ login })
      setMessage(data.message)
      if (data.resetToken) setResetToken(data.resetToken)
    } catch (err) {
      setError(err.response?.data?.error || 'Request failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page">
      <div className="shell">
        <p className="brand">Auth Module</p>
        <h1>Forgot password</h1>
        <p className="lead">Enter your username, email, or phone.</p>
        <form onSubmit={onSubmit} className="stack">
          <label>
            Login
            <input value={login} onChange={(e) => setLogin(e.target.value)} required />
          </label>
          {message && <p className="ok">{message}</p>}
          {resetToken && (
            <p className="ok">
              Demo reset token: <code>{resetToken}</code>
              <br />
              <Link to={`/reset-password?token=${encodeURIComponent(resetToken)}`}>Continue to reset</Link>
            </p>
          )}
          {error && <p className="error">{error}</p>}
          <button type="submit" disabled={busy}>{busy ? 'Sending…' : 'Send reset token'}</button>
        </form>
        <div className="links">
          <Link to="/login">Back to login</Link>
        </div>
      </div>
    </div>
  )
}
