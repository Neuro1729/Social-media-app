import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { socialApi } from './SocialApi'

export default function EditProfilePage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [bio, setBio] = useState('')
  const [profilePictureUrl, setProfilePictureUrl] = useState('')
  const [isPrivate, setIsPrivate] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!user?.username) return
    socialApi.getProfile(user.username)
      .then((profile) => {
        setBio(profile.bio || '')
        setProfilePictureUrl(profile.profilePictureUrl || '')
        setIsPrivate(Boolean(profile.isPrivate))
      })
      .catch(() => {})
  }, [user?.username])

  async function saveProfile(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    setMessage('')
    try {
      await socialApi.updateProfile({ bio, profilePictureUrl })
      setMessage('Profile updated')
    } catch (err) {
      setError(err.response?.data?.error || 'Update failed')
    } finally {
      setBusy(false)
    }
  }

  async function savePrivacy(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    setMessage('')
    try {
      await socialApi.changePrivacy(isPrivate)
      setMessage('Privacy updated')
    } catch (err) {
      setError(err.response?.data?.error || 'Privacy update failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page">
      <div className="shell">
        <p className="brand">Canopy</p>
        <hr className="divider" />
        <nav className="side-nav">
          <Link to={user?.username ? `/profile/${user.username}` : '/account'}>My Profile</Link>
          <Link to="/account">Account</Link>
        </nav>
        <hr className="divider" />
        <h1>Edit profile</h1>
        <p className="lead">Update how you appear to others.</p>
        <form className="stack" onSubmit={saveProfile}>
          <label>
            Bio
            <input value={bio} maxLength={160} onChange={(e) => setBio(e.target.value)} />
          </label>
          <label>
            Profile picture URL
            <input value={profilePictureUrl} onChange={(e) => setProfilePictureUrl(e.target.value)} placeholder="https://..." />
          </label>
          <button type="submit" disabled={busy}>Save Profile</button>
        </form>
        <hr className="divider" />
        <form className="stack" onSubmit={savePrivacy}>
          <label className="row" style={{ alignItems: 'center' }}>
            <input type="checkbox" checked={isPrivate} onChange={(e) => setIsPrivate(e.target.checked)} />
            Private account
          </label>
          <button type="submit" className="secondary" disabled={busy}>Save Privacy</button>
        </form>
        {message && <p className="ok">{message}</p>}
        {error && <p className="error">{error}</p>}
        {!user?.username && (
          <p className="lead">Set a username on the account page before viewing your public profile URL.</p>
        )}
        <button type="button" className="secondary" style={{ marginTop: '1rem' }} onClick={() => navigate('/account')}>
          Account settings
        </button>
      </div>
    </div>
  )
}
