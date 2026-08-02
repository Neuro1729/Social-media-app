import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'

export default function InfiniteUserList({
  fetchPage,
  renderActions,
  emptyText = 'No users yet',
}) {
  const [items, setItems] = useState([])
  const [cursor, setCursor] = useState(null)
  const [hasMore, setHasMore] = useState(true)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const seen = useRef(new Set())
  const sentinel = useRef(null)
  const loadingRef = useRef(false)

  const loadMore = useCallback(async () => {
    if (loadingRef.current || !hasMore) return
    loadingRef.current = true
    setLoading(true)
    setError('')
    try {
      const page = await fetchPage(cursor)
      const nextItems = []
      for (const item of page.items || []) {
        if (!seen.current.has(item.userId)) {
          seen.current.add(item.userId)
          nextItems.push(item)
        }
      }
      setItems((prev) => [...prev, ...nextItems])
      setCursor(page.nextCursor || null)
      setHasMore(Boolean(page.hasMore))
    } catch (err) {
      setError(err.response?.data?.error || 'Could not load list')
      setHasMore(false)
    } finally {
      setLoading(false)
      loadingRef.current = false
    }
  }, [cursor, fetchPage, hasMore])

  useEffect(() => {
    seen.current = new Set()
    setItems([])
    setCursor(null)
    setHasMore(true)
    loadingRef.current = false
  }, [fetchPage])

  useEffect(() => {
    if (items.length === 0 && hasMore && !loadingRef.current) {
      loadMore()
    }
  }, [items.length, hasMore, loadMore])

  useEffect(() => {
    const node = sentinel.current
    if (!node) return undefined
    const observer = new IntersectionObserver((entries) => {
      if (entries[0]?.isIntersecting) {
        loadMore()
      }
    }, { rootMargin: '120px' })
    observer.observe(node)
    return () => observer.disconnect()
  }, [loadMore])

  return (
    <div className="infinite-list">
      {error && <p className="error">{error}</p>}
      {items.length === 0 && !loading && !error && <p className="lead">{emptyText}</p>}
      <ul className="card-list">
        {items.map((item) => (
          <li key={item.userId} className="card">
            <div className="user-row">
              {item.profilePictureUrl ? (
                <img src={item.profilePictureUrl} alt="" className="avatar" />
              ) : (
                <div className="avatar placeholder" />
              )}
              <div>
                <Link to={`/profile/${encodeURIComponent(item.username || item.userId)}`}>
                  <strong>{item.username || 'unknown'}</strong>
                </Link>
                {item.isPrivate && <span className="badge">Private</span>}
                {item.relationshipStatus && item.relationshipStatus !== 'NONE' && (
                  <div className="relation-chip">{item.relationshipStatus}</div>
                )}
              </div>
            </div>
            {renderActions?.(item, {
              remove: (userId) => setItems((prev) => prev.filter((x) => x.userId !== userId)),
            })}
          </li>
        ))}
      </ul>
      <div ref={sentinel} />
      {loading && <p className="lead">Loading…</p>}
    </div>
  )
}
