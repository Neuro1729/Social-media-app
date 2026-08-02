import { useCallback } from 'react'
import { Link, useParams } from 'react-router-dom'
import InfiniteUserList from './InfiniteUserList'
import { socialApi } from './SocialApi'

export default function FollowingPage() {
  const { username } = useParams()
  const fetchPage = useCallback(
    (cursor) => socialApi.following(username, cursor),
    [username],
  )

  return (
    <div className="page">
      <div className="shell wide">
        <p className="brand">Canopy</p>
        <hr className="divider" />
        <h1>Following</h1>
        <p className="lead">
          <Link to={`/profile/${encodeURIComponent(username)}`}>← Back to @{username}</Link>
        </p>
        <InfiniteUserList fetchPage={fetchPage} emptyText="Not following anyone yet" />
      </div>
    </div>
  )
}
