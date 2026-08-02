import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from './AuthProvider'

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', email: '', phone: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function update(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function onSubmit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await register({
        username: form.username || null,
        email: form.email || null,
        phone: form.phone || null,
        password: form.password,
      })
      navigate('/account')
    } catch (err) {
      setError(err.response?.data?.error || 'Registration failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page">
      <div className="shell">
        <p className="brand">Auth Module</p>
        <h1>Create account</h1>
        <p className="lead">Email or phone required. Username optional.</p>
        <form onSubmit={onSubmit} className="stack">
          <label>
            Username
            <input value={form.username} onChange={(e) => update('username', e.target.value)} autoComplete="username" />
          </label>
          <label>
            Email
            <input type="email" value={form.email} onChange={(e) => update('email', e.target.value)} autoComplete="email" />
          </label>
          <label>
            Phone
            <input value={form.phone} onChange={(e) => update('phone', e.target.value)} autoComplete="tel" />
          </label>
          <label>
            Password
            <input type="password" value={form.password} onChange={(e) => update('password', e.target.value)} required minLength={8} autoComplete="new-password" />
          </label>
          {error && <p className="error">{error}</p>}
          <button type="submit" disabled={busy}>{busy ? 'Creating…' : 'Create account'}</button>
        </form>
        <div className="links">
          <Link to="/login">Already have an account?</Link>
        </div>
      </div>
    </div>
  )
}
