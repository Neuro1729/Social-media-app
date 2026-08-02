import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import InfiniteUserList from './InfiniteUserList'
import { socialApi } from './SocialApi'

export default function BlockedUsersPage() {
  const [busyId, setBusyId] = useState(null)
  const fetchPage = useCallback((cursor) => socialApi.blockedUsers(cursor), [])

  return (
    <div className="page">
      <div className="shell wide">
        <p className="brand">Canopy</p>
        <hr className="divider" />
        <nav className="side-nav">
          <Link to="/account">Account</Link>
        </nav>
        <hr className="divider" />
        <h1>Blocked users</h1>
        <p className="lead">Accounts you have blocked.</p>
        <InfiniteUserList
          fetchPage={fetchPage}
          emptyText="No blocked users"
          renderActions={(item, { remove }) => (
            <button
              type="button"
              className="secondary"
              disabled={busyId === item.userId}
              onClick={async () => {
                setBusyId(item.userId)
                try {
                  await socialApi.unblock(item.userId)
                  remove(item.userId)
                } finally {
                  setBusyId(null)
                }
              }}
            >
              Unblock
            </button>
          )}
        />
      </div>
    </div>
  )
}
