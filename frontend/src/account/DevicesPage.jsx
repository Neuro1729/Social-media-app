import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { accountApi } from './accountApi'

function deviceTitle(name) {
  if (!name) return 'Unknown device'
  if (name.length > 48) return `${name.slice(0, 48)}…`
  return name
}

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
        <p className="brand">Canopy</p>
        <hr className="divider" />
        <nav className="side-nav">
          <Link to="/account">Account</Link>
        </nav>
        <hr className="divider" />
        <h1>Devices</h1>
        <p className="lead">Sessions signed in across your devices.</p>
        {error && <p className="error">{error}</p>}
        <ul className="card-list">
          {sessions.map((session) => (
            <li key={session.sessionId} className="card">
              <div>
                <strong>{deviceTitle(session.deviceName)}</strong>
                {session.current && <span className="badge">Current Device</span>}
                <p>Started {new Date(session.createdAt).toLocaleString()}</p>
                <p>Expires {new Date(session.expiresAt).toLocaleString()}</p>
              </div>
              <button type="button" className="secondary" disabled={busy} onClick={() => revoke(session.sessionId)}>
                Logout
              </button>
            </li>
          ))}
        </ul>
        <button type="button" className="danger" disabled={busy || sessions.length === 0} onClick={revokeAll}>
          Logout All Devices
        </button>
      </div>
    </div>
  )
}
