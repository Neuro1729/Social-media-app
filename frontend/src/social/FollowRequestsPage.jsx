import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import InfiniteUserList from './InfiniteUserList'
import { socialApi } from './SocialApi'

export default function FollowRequestsPage() {
  const [busyId, setBusyId] = useState(null)
  const fetchPage = useCallback((cursor) => socialApi.followRequests(cursor), [])

  return (
    <div className="page">
      <div className="shell wide">
        <p className="brand">Canopy</p>
        <hr className="divider" />
        <nav className="side-nav">
          <Link to="/account">Account</Link>
        </nav>
        <hr className="divider" />
        <h1>Follow requests</h1>
        <p className="lead">People waiting for approval.</p>
        <InfiniteUserList
          fetchPage={fetchPage}
          emptyText="No pending requests"
          renderActions={(item, { remove }) => (
            <div className="row">
              <button
                type="button"
                disabled={busyId === item.userId}
                onClick={async () => {
                  setBusyId(item.userId)
                  try {
                    await socialApi.approveRequest(item.userId)
                    remove(item.userId)
                  } finally {
                    setBusyId(null)
                  }
                }}
              >
                ✓ Approve
              </button>
              <button
                type="button"
                className="danger"
                disabled={busyId === item.userId}
                onClick={async () => {
                  setBusyId(item.userId)
                  try {
                    await socialApi.rejectRequest(item.userId)
                    remove(item.userId)
                  } finally {
                    setBusyId(null)
                  }
                }}
              >
                ✕ Reject
              </button>
            </div>
          )}
        />
      </div>
    </div>
  )
}
