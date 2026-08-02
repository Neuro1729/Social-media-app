import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { accountApi } from './accountApi'

export default function DevicesPage() {
  const { logout } = useAuth()
  const [sessions, setSessions] = useState([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function load() {
    try {
      const data = await accountApi.sessions()
      setSessions(data.sessions || [])
    } catch (err) {
      setError(err.response?.data?.error || 'Could not load sessions')
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function revoke(sessionId) {
    setBusy(true)
    setError('')
    try {
      await accountApi.revokeSession(sessionId)
      await load()
    } catch (err) {
      setError(err.response?.data?.error || 'Could not revoke session')
    } finally {
      setBusy(false)
    }
  }

  async function revokeAll() {
    setBusy(true)
    setError('')
    try {
      await accountApi.revokeAllSessions()
      await logout()
    } catch (err) {
      setError(err.response?.data?.error || 'Could not revoke sessions')
      setBusy(false)
    }
  }

  return (
    <div className="page">
      <div className="shell wide">
        <header className="topbar">
          <p className="brand">Auth Module</p>
          <nav className="links">
            <Link to="/account">Account</Link>
          </nav>
        </header>
        <h1>Devices</h1>
        <p className="lead">Active refresh sessions across your devices.</p>
        {error && <p className="error">{error}</p>}
        <ul className="device-list">
          {sessions.map((session) => (
            <li key={session.sessionId}>
              <div>
                <strong>{session.deviceName}</strong>
                {session.current && <span className="badge">Current</span>}
                <p>Created {new Date(session.createdAt).toLocaleString()}</p>
                <p>Expires {new Date(session.expiresAt).toLocaleString()}</p>
              </div>
              <button type="button" className="secondary" disabled={busy} onClick={() => revoke(session.sessionId)}>
                Logout device
              </button>
            </li>
          ))}
        </ul>
        <button type="button" disabled={busy || sessions.length === 0} onClick={revokeAll}>
          Logout all devices
        </button>
      </div>
    </div>
  )
}
