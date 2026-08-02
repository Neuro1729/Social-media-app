import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { profileLoadError } from '../api/profileErrors'
import { socialApi } from './SocialApi'

export default function ProfileSearchBar() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    const value = username.trim()
    if (!value) return
    setBusy(true)
    setError('')
    try {
      const profile = await socialApi.search(value)
      navigate(`/profile/${encodeURIComponent(profile.username || value.toLowerCase())}`)
    } catch (err) {
      setError(profileLoadError(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="search-bar" onSubmit={onSubmit}>
      <input
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        placeholder="🔍 Search username..."
        aria-label="Search username"
      />
      <button type="submit" disabled={busy}>{busy ? '…' : 'Search'}</button>
      {error && <p className="error search-error">{error}</p>}
    </form>
  )
}
