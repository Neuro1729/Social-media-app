import { useState } from 'react'
import { postApi } from './postApi'

export default function CreatePostForm({ onCreated }) {
  const [body, setBody] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    const trimmed = body.trim()
    if (!trimmed) {
      setError('Post cannot be blank')
      return
    }
    if (trimmed.length > 2000) {
      setError('Post is too long')
      return
    }
    setBusy(true)
    setError('')
    try {
      const post = await postApi.create(trimmed)
      setBody('')
      onCreated?.(post)
    } catch (err) {
      setError(err.response?.data?.error || 'Could not create post')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="stack post-compose" onSubmit={onSubmit}>
      <label>
        New post
        <textarea
          value={body}
          maxLength={2000}
          rows={3}
          placeholder="Share something with Canopy…"
          onChange={(e) => setBody(e.target.value)}
          disabled={busy}
        />
      </label>
      <div className="row post-compose-actions">
        <span className="lead" style={{ margin: 0 }}>{body.trim().length}/2000</span>
        <button type="submit" disabled={busy || !body.trim()}>
          {busy ? 'Posting…' : 'Post'}
        </button>
      </div>
      {error && <p className="error">{error}</p>}
    </form>
  )
}
