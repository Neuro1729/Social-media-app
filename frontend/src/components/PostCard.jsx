import { useState } from "react";
import { Link } from "react-router-dom";

export default function PostCard({ post, onEdit, onDelete }) {
  const [editing, setEditing] = useState(false);
  const [caption, setCaption] = useState(post.caption || "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function handleSave(e) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      await onEdit(post.id, caption.trim());
      setEditing(false);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleDelete() {
    if (!window.confirm("Delete this post?")) return;
    setError("");
    setBusy(true);
    try {
      await onDelete(post.id);
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  }

  return (
    <article className="post-card">
      <header className="post-card-head">
        <Link to={`/users/${post.ownerUsername}`} className="post-author">
          @{post.ownerUsername}
        </Link>
        <time dateTime={post.createdAt}>
          {new Date(post.createdAt).toLocaleString()}
        </time>
      </header>

      {post.imageUrl && (
        <div className="post-image">
          <img src={post.imageUrl} alt="" />
        </div>
      )}

      {editing ? (
        <form className="stack-form" onSubmit={handleSave}>
          <textarea
            value={caption}
            onChange={(e) => setCaption(e.target.value)}
            rows={3}
            maxLength={2200}
            required
          />
          {error && <p className="error">{error}</p>}
          <div className="post-actions">
            <button type="submit" disabled={busy}>
              {busy ? "Saving…" : "Save"}
            </button>
            <button
              type="button"
              className="ghost-btn"
              disabled={busy}
              onClick={() => {
                setEditing(false);
                setCaption(post.caption || "");
                setError("");
              }}
            >
              Cancel
            </button>
          </div>
        </form>
      ) : (
        <>
          {post.caption ? <p className="post-caption">{post.caption}</p> : null}
          {error && <p className="error">{error}</p>}
          {post.mine && (
            <div className="post-actions">
              <button
                type="button"
                className="ghost-btn"
                disabled={busy}
                onClick={() => setEditing(true)}
              >
                Edit caption
              </button>
              <button
                type="button"
                className="danger-btn"
                disabled={busy}
                onClick={handleDelete}
              >
                Delete
              </button>
            </div>
          )}
        </>
      )}
    </article>
  );
}
