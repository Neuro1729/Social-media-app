import { Link } from 'react-router-dom'
import LikeButton from './LikeButton'

export default function PostCard({ post, onDeleted }) {
  async function handleDelete() {
    if (!post.canDelete) return
    if (!window.confirm('Delete this post?')) return
    onDeleted?.(post.id)
  }

  return (
    <article className="post-card">
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
          <button type="button" className="danger" onClick={handleDelete}>
            Delete
          </button>
        )}
      </div>
      <p className="post-body">{post.body}</p>
      <div className="row post-card-actions">
        <LikeButton
          postId={post.id}
          initialLiked={post.likedByViewer}
          initialCount={post.likeCount}
        />
        <Link className="secondary-link" to={`/posts/${post.id}`}>
          Comments · {post.commentCount ?? 0}
        </Link>
      </div>
    </article>
  )
}
