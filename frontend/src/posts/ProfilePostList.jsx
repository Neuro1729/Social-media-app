import { useCallback, useEffect, useRef, useState } from 'react'
import PostCard from './PostCard'
import { postApi } from './postApi'

export default function ProfilePostList({ username, refreshKey = 0 }) {
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
      const page = await postApi.profilePosts(username, cursor)
      const next = []
      for (const item of page.items || []) {
        if (!seen.current.has(item.id)) {
          seen.current.add(item.id)
          next.push(item)
        }
      }
      setItems((prev) => [...prev, ...next])
      setCursor(page.nextCursor || null)
      setHasMore(Boolean(page.hasMore))
    } catch (err) {
      setError(err.response?.data?.error || 'Could not load posts')
      setHasMore(false)
    } finally {
      setLoading(false)
      loadingRef.current = false
    }
  }, [cursor, hasMore, username])

  useEffect(() => {
    seen.current = new Set()
    setItems([])
    setCursor(null)
    setHasMore(true)
    loadingRef.current = false
  }, [username, refreshKey])

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

  async function onDeleted(postId) {
    try {
      await postApi.remove(postId)
      setItems((prev) => prev.filter((p) => p.id !== postId))
      seen.current.delete(postId)
    } catch (err) {
      setError(err.response?.data?.error || 'Could not delete post')
    }
  }

  return (
    <section className="profile-posts">
      <h2>Posts</h2>
      {error && <p className="error">{error}</p>}
      {items.length === 0 && !loading && !error && (
        <p className="lead">No posts yet.</p>
      )}
      <div className="post-list">
        {items.map((post) => (
          <PostCard key={post.id} post={post} onDeleted={onDeleted} />
        ))}
      </div>
      <div ref={sentinel} />
      {loading && <p className="lead">Loading posts…</p>}
    </section>
  )
}
