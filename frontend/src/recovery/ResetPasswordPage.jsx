import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { authApi } from '../auth/authApi'

export default function ResetPasswordPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const [token, setToken] = useState(params.get('token') || '')
  const [newPassword, setNewPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await authApi.resetPassword({ token, newPassword })
      navigate('/login')
    } catch (err) {
      setError(err.response?.data?.error || 'Reset failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page">
      <div className="shell">
        <p className="brand">Canopy</p>
        <h1>Reset password</h1>
        <p className="lead">Paste your reset token and choose a new password.</p>
        <form onSubmit={onSubmit} className="stack">
          <label>
            Reset token
            <input value={token} onChange={(e) => setToken(e.target.value)} required />
          </label>
          <label>
            New password
            <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required minLength={8} />
          </label>
          {error && <p className="error">{error}</p>}
          <button type="submit" disabled={busy}>{busy ? 'Updating…' : 'Update password'}</button>
        </form>
        <div className="links">
          <Link to="/login">Back to login</Link>
        </div>
      </div>
    </div>
  )
}
