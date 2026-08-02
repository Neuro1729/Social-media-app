import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import CreatePostForm from '../posts/CreatePostForm'
import ProfilePostList from '../posts/ProfilePostList'
import ProfileSearchBar from './ProfileSearchBar'
import { socialApi } from './SocialApi'

export default function ProfilePage() {
  const { username } = useParams()
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [postsRefreshKey, setPostsRefreshKey] = useState(0)

  const load = useCallback(async () => {
    setError('')
    try {
      const data = await socialApi.getProfile(username)
      setProfile(data)
    } catch (err) {
      setProfile(null)
      setError(err.response?.data?.error || 'User not found')
    }
  }, [username])

  useEffect(() => {
    load()
  }, [load])

  async function onFollow() {
    setBusy(true)
    try {
      await socialApi.follow(profile.userId)
      await load()
    } catch (err) {
      setError(err.response?.data?.error || 'Follow failed')
    } finally {
      setBusy(false)
    }
  }

  async function onUnfollow() {
    setBusy(true)
    try {
      await socialApi.unfollow(profile.userId)
      await load()
    } catch (err) {
      setError(err.response?.data?.error || 'Unfollow failed')
    } finally {
      setBusy(false)
    }
  }

  async function onBlock() {
    if (!window.confirm('Block this user? Follow relationships will be removed.')) return
    setBusy(true)
    try {
      await socialApi.block(profile.userId)
      await load()
    } catch (err) {
      setError(err.response?.data?.error || 'Block failed')
    } finally {
      setBusy(false)
    }
  }

  const isPrivateLocked = profile
    && profile.isPrivate
    && !profile.canViewProtectedContent
    && profile.relationshipStatus !== 'SELF'

  return (
    <div className="page">
      <div className="shell wide">
        <p className="brand">Canopy</p>
        <hr className="divider" />
        <ProfileSearchBar />
        <hr className="divider" />
        <nav className="side-nav">
          <Link to="/account">Account</Link>
          <Link to="/profile/edit">Edit Profile</Link>
          <Link to="/follow-requests">Requests</Link>
          <Link to="/blocked-users">Blocked</Link>
        </nav>
        <hr className="divider" />
        {error && <p className="error">{error}</p>}
        {profile && (
          <>
            <div className="profile-panel">
              {profile.profilePictureUrl ? (
                <img src={profile.profilePictureUrl} alt="" className="avatar large" />
              ) : (
                <div className="avatar large placeholder" />
              )}
              <h1>@{profile.username || 'no-username'}</h1>
              {!isPrivateLocked && (
                <p className="lead" style={{ marginBottom: 0 }}>{profile.bio || 'No bio yet.'}</p>
              )}
              <p className="lead" style={{ margin: '0.2rem 0 0' }}>
                {profile.isPrivate ? 'Private Account' : 'Public Account'}
              </p>

              {isPrivateLocked ? (
                <>
                  <p className="lead">Only approved followers can view this profile.</p>
                  <div className="profile-actions">
                    {(profile.relationshipStatus === 'NONE' || profile.relationshipStatus === 'REJECTED') && (
                      <button type="button" disabled={busy} onClick={onFollow}>Request Follow</button>
                    )}
                    {profile.relationshipStatus === 'PENDING' && (
                      <button type="button" className="secondary" disabled={busy} onClick={onUnfollow}>
                        Cancel Request
                      </button>
                    )}
                    <button type="button" className="danger" disabled={busy} onClick={onBlock}>Block</button>
                  </div>
                </>
              ) : (
                <>
                  <div className="profile-counts">
                    <Link to={`/profile/${encodeURIComponent(profile.username)}/followers`}>
                      <strong>{profile.followerCount}</strong>
                      <span>Followers</span>
                    </Link>
                    <Link to={`/profile/${encodeURIComponent(profile.username)}/following`}>
                      <strong>{profile.followingCount}</strong>
                      <span>Following</span>
                    </Link>
                  </div>
                  {profile.relationshipStatus === 'SELF' ? (
                    <p className="ok">This is your profile.</p>
                  ) : (
                    <div className="profile-actions">
                      {(profile.relationshipStatus === 'NONE' || profile.relationshipStatus === 'REJECTED') && (
                        <button type="button" disabled={busy} onClick={onFollow}>
                          {profile.isPrivate ? 'Request Follow' : 'Follow'}
                        </button>
                      )}
                      {profile.relationshipStatus === 'PENDING' && (
                        <button type="button" className="secondary" disabled={busy} onClick={onUnfollow}>
                          Cancel Request
                        </button>
                      )}
                      {profile.relationshipStatus === 'FOLLOWING' && (
                        <button type="button" className="secondary" disabled={busy} onClick={onUnfollow}>
                          Following
                        </button>
                      )}
                      <button type="button" className="danger" disabled={busy} onClick={onBlock}>Block</button>
                    </div>
                  )}
                </>
              )}
            </div>

            {!isPrivateLocked && (
              <>
                <hr className="divider" />
                {profile.relationshipStatus === 'SELF' && (
                  <CreatePostForm onCreated={() => setPostsRefreshKey((k) => k + 1)} />
                )}
                <ProfilePostList
                  username={profile.username}
                  refreshKey={postsRefreshKey}
                />
              </>
            )}
          </>
        )}
      </div>
    </div>
  )
}
