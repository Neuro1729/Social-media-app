import { useState } from 'react'
import { postApi } from './postApi'

export default function LikeButton({ postId, initialLiked, initialCount, onChange }) {
  const [liked, setLiked] = useState(Boolean(initialLiked))
  const [count, setCount] = useState(initialCount ?? 0)
  const [busy, setBusy] = useState(false)

  async function toggle() {
    if (busy) return
    setBusy(true)
    try {
      const result = liked
        ? await postApi.unlike(postId)
        : await postApi.like(postId)
      setLiked(result.likedByViewer)
      setCount(result.likeCount)
      onChange?.(result)
    } catch {
      // Keep prior local state on failure.
    } finally {
      setBusy(false)
    }
  }

  return (
    <button
      type="button"
      className={liked ? 'secondary like-btn liked' : 'secondary like-btn'}
      onClick={toggle}
      disabled={busy}
      aria-pressed={liked}
    >
      {liked ? 'Liked' : 'Like'} · {count}
    </button>
  )
}
