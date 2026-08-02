import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import CommentSection from './CommentSection'
import LikeButton from './LikeButton'
import { postApi } from './postApi'

export default function PostPage() {
  const { id } = useParams()
  const [post, setPost] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    setError('')
    try {
      const data = await postApi.get(id)
      setPost(data)
    } catch (err) {
      setPost(null)
      setError(err.response?.data?.error || 'Post not found')
    }
  }, [id])

  useEffect(() => {
    load()
  }, [load])

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
                initialLiked={post.likedByViewer}
                initialCount={post.likeCount}
              />
            </div>
            <hr className="divider" />
            <CommentSection postId={post.id} />
          </article>
        )}
      </div>
    </div>
  )
}
