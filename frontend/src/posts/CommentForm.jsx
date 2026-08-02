import { useState } from 'react'
import { postApi } from './postApi'

export default function CommentForm({ postId, onCreated }) {
  const [body, setBody] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    const trimmed = body.trim()
    if (!trimmed) {
      setError('Comment cannot be blank')
      return
    }
    if (trimmed.length > 1000) {
      setError('Comment is too long')
      return
    }
    setBusy(true)
    setError('')
    try {
      const comment = await postApi.createComment(postId, trimmed)
      setBody('')
      onCreated?.(comment)
    } catch (err) {
      setError(err.response?.data?.error || 'Could not add comment')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="stack" onSubmit={onSubmit}>
      <label>
        Add a comment
        <textarea
          value={body}
          maxLength={1000}
          rows={2}
          placeholder="Write a comment…"
          onChange={(e) => setBody(e.target.value)}
          disabled={busy}
        />
      </label>
      <div className="row">
        <button type="submit" disabled={busy || !body.trim()}>
          {busy ? 'Sending…' : 'Comment'}
        </button>
      </div>
      {error && <p className="error">{error}</p>}
    </form>
  )
}
