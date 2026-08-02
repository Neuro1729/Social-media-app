import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { accountApi } from './accountApi'
import ProfileSearchBar from '../social/ProfileSearchBar'

export default function AccountPage() {
  const { user, logout, refreshUser } = useAuth()
  const [username, setUsername] = useState(user?.username || '')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function changeUsername(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    setMessage('')
    try {
      await accountApi.changeUsername(username)
      await refreshUser()
      setMessage('Username updated')
    } catch (err) {
      setError(err.response?.data?.error || 'Could not update username')
    } finally {
      setBusy(false)
    }
  }

  async function removeUsername() {
    setBusy(true)
    setError('')
    setMessage('')
    try {
      await accountApi.removeUsername()
      setUsername('')
      await refreshUser()
      setMessage('Username removed (old name stays reserved)')
    } catch (err) {
      setError(err.response?.data?.error || 'Could not remove username')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page">
      <div className="shell wide">
        <p className="brand">Canopy</p>
        <hr className="divider" />
        <ProfileSearchBar />
        <hr className="divider" />
        <nav className="side-nav">
          {user?.username && <Link to={`/profile/${user.username}`}>My Profile</Link>}
          <Link to="/profile/edit">Edit Profile</Link>
          <Link to="/follow-requests">Requests</Link>
          <Link to="/blocked-users">Blocked</Link>
          <Link to="/account/devices">Devices</Link>
          <button type="button" className="linkish logout" onClick={logout}>Logout</button>
        </nav>
        <hr className="divider" />
        <h1>Account</h1>
        <p className="lead">Signed in as {user?.email || user?.phone || user?.username}</p>
        <dl className="meta">
          <div><dt>User ID</dt><dd>{user?.id}</dd></div>
          <div><dt>Username</dt><dd>{user?.username || '—'}</dd></div>
          <div><dt>Email</dt><dd>{user?.email || '—'}</dd></div>
          <div><dt>Phone</dt><dd>{user?.phone || '—'}</dd></div>
        </dl>
        <form onSubmit={changeUsername} className="stack">
          <label>
            Change username
            <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="new_username" />
          </label>
          <div className="row">
            <button type="submit" disabled={busy}>Save username</button>
            <button type="button" className="secondary" disabled={busy || !user?.username} onClick={removeUsername}>
              Remove username
            </button>
          </div>
        </form>
        {message && <p className="ok">{message}</p>}
        {error && <p className="error">{error}</p>}
      </div>
    </div>
  )
}
