import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import CommentForm from './CommentForm'
import { postApi } from './postApi'

export default function CommentSection({ postId, liveEvent, viewerId, postAuthorId }) {
  const [items, setItems] = useState([])
  const [cursor, setCursor] = useState(null)
  const [hasMore, setHasMore] = useState(true)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const seen = useRef(new Set())
  const loadingRef = useRef(false)

  const withDeleteFlag = useCallback((comment) => ({
    ...comment,
    canDelete: Boolean(
      comment?.canDelete
      || (viewerId && comment?.authorId === viewerId)
      || (viewerId && postAuthorId && postAuthorId === viewerId),
    ),
  }), [postAuthorId, viewerId])

  const upsertComment = useCallback((comment) => {
    if (!comment?.id || seen.current.has(comment.id)) return
    seen.current.add(comment.id)
    const next = withDeleteFlag(comment)
    setItems((prev) => [next, ...prev])
  }, [withDeleteFlag])

  const removeComment = useCallback((commentId) => {
    if (!commentId) return
    seen.current.delete(commentId)
    setItems((prev) => prev.filter((c) => c.id !== commentId))
  }, [])

  useEffect(() => {
    if (!liveEvent) return
    if (liveEvent.type === 'comment_created') {
      upsertComment(liveEvent.comment)
    } else if (liveEvent.type === 'comment_deleted') {
      removeComment(liveEvent.commentId)
    }
  }, [liveEvent, removeComment, upsertComment])

  const loadMore = useCallback(async () => {
    if (loadingRef.current || !hasMore) return
    loadingRef.current = true
    setLoading(true)
    setError('')
    try {
      const page = await postApi.comments(postId, cursor)
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
      setError(err.response?.data?.error || 'Could not load comments')
      setHasMore(false)
    } finally {
      setLoading(false)
      loadingRef.current = false
    }
  }, [cursor, hasMore, postId])

  useEffect(() => {
    seen.current = new Set()
    setItems([])
    setCursor(null)
    setHasMore(true)
    loadingRef.current = false
  }, [postId])

  useEffect(() => {
    if (items.length === 0 && hasMore && !loadingRef.current) {
      loadMore()
    }
  }, [items.length, hasMore, loadMore])

  async function onDelete(commentId) {
    try {
      await postApi.deleteComment(commentId)
      removeComment(commentId)
    } catch (err) {
      setError(err.response?.data?.error || 'Could not delete comment')
    }
  }

  function onCreated(comment) {
    upsertComment(comment)
  }

  return (
    <section className="comment-section">
      <h2>Comments</h2>
      <CommentForm postId={postId} onCreated={onCreated} />
      {error && <p className="error">{error}</p>}
      {items.length === 0 && !loading && !error && (
        <p className="lead">No comments yet.</p>
      )}
      <ul className="comment-list">
        {items.map((comment) => (
          <li key={comment.id} className="comment-item">
            <div className="user-row">
              {comment.authorProfilePictureUrl ? (
                <img src={comment.authorProfilePictureUrl} alt="" className="avatar" />
              ) : (
                <div className="avatar placeholder" />
              )}
              <div>
                <Link to={`/profile/${encodeURIComponent(comment.authorUsername || '')}`}>
                  <strong>@{comment.authorUsername || 'user'}</strong>
                </Link>
                <p>{comment.body}</p>
                {comment.canDelete && (
                  <button
                    type="button"
                    className="linkish"
                    onClick={() => onDelete(comment.id)}
                  >
                    Delete
                  </button>
                )}
              </div>
            </div>
          </li>
        ))}
      </ul>
      {hasMore && (
        <button type="button" className="secondary" disabled={loading} onClick={loadMore}>
          {loading ? 'Loading…' : 'Load more comments'}
        </button>
      )}
    </section>
  )
}
