import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import CommentSection from './CommentSection'
import LikeButton from './LikeButton'
import { postApi } from './postApi'
import { subscribePostEvents } from './postLive'

export default function PostPage() {
  const { id } = useParams()
  const { accessToken, user } = useAuth()
  const [post, setPost] = useState(null)
  const [likeCount, setLikeCount] = useState(0)
  const [likedByViewer, setLikedByViewer] = useState(false)
  const [liveCommentEvent, setLiveCommentEvent] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    setError('')
    try {
      const data = await postApi.get(id)
      setPost(data)
      setLikeCount(data.likeCount ?? 0)
      setLikedByViewer(Boolean(data.likedByViewer))
    } catch (err) {
      setPost(null)
      setError(err.response?.data?.error || 'Post not found')
    }
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (!post?.id || !accessToken) return undefined
    return subscribePostEvents(post.id, accessToken, {
      onLike: (payload) => {
        if (typeof payload?.likeCount === 'number') {
          setLikeCount(payload.likeCount)
        }
        if (payload?.actorId && user?.id && payload.actorId === user.id) {
          setLikedByViewer(Boolean(payload.liked))
        }
      },
      onCommentCreated: (payload) => {
        if (!payload?.comment) return
        setLiveCommentEvent({
          type: 'comment_created',
          comment: payload.comment,
          key: `${payload.comment.id}-${Date.now()}`,
        })
      },
      onCommentDeleted: (payload) => {
        if (!payload?.commentId) return
        setLiveCommentEvent({
          type: 'comment_deleted',
          commentId: payload.commentId,
          key: `${payload.commentId}-${Date.now()}`,
        })
      },
    })
  }, [accessToken, post?.id, user?.id])

  async function onDelete() {
    if (!post?.canDelete) return
    if (!window.confirm('Delete this post?')) return
    setBusy(true)
    try {
      await postApi.remove(post.id)
      window.history.back()
    } catch (err) {
      setError(err.response?.data?.error || 'Could not delete post')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page">
      <div className="shell wide">
        <p className="brand">Canopy</p>
        <hr className="divider" />
        <nav className="side-nav">
          {post?.authorUsername && (
            <Link to={`/profile/${encodeURIComponent(post.authorUsername)}`}>
              Back to profile
            </Link>
          )}
          <Link to="/account">Account</Link>
        </nav>
        <hr className="divider" />
        {error && <p className="error">{error}</p>}
        {post && (
          <article className="post-detail">
            <div className="post-card-head">
              <div className="user-row">
                {post.authorProfilePictureUrl ? (
                  <img src={post.authorProfilePictureUrl} alt="" className="avatar" />
                ) : (
                  <div className="avatar placeholder" />
                )}
                <div>
                  <Link to={`/profile/${encodeURIComponent(post.authorUsername || '')}`}>
                    <strong>@{post.authorUsername || 'user'}</strong>
                  </Link>
                  <div className="relation-chip">
                    {post.createdAt ? new Date(post.createdAt).toLocaleString() : ''}
                  </div>
                </div>
              </div>
              {post.canDelete && (
                <button type="button" className="danger" disabled={busy} onClick={onDelete}>
                  Delete
                </button>
              )}
            </div>
            <p className="post-body large">{post.body}</p>
            <div className="row post-card-actions">
              <LikeButton
                postId={post.id}
                initialLiked={likedByViewer}
                initialCount={likeCount}
                onChange={(result) => {
                  setLikedByViewer(Boolean(result.likedByViewer))
                  setLikeCount(result.likeCount ?? 0)
                }}
              />
            </div>
            <hr className="divider" />
            <CommentSection
              postId={post.id}
              liveEvent={liveCommentEvent}
              viewerId={user?.id}
              postAuthorId={post.authorId}
            />
          </article>
        )}
      </div>
    </div>
  )
}
