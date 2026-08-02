/**
 * Subscribe to post engagement SSE with Authorization header support.
 * Updates only like/comment UI — not a full page reload.
 */
export function subscribePostEvents(postId, accessToken, handlers = {}) {
  if (!postId || !accessToken) {
    return () => {}
  }

  const controller = new AbortController()
  const apiOrigin = import.meta.env.VITE_API_ORIGIN || ''
  const url = `${apiOrigin}/api/posts/${encodeURIComponent(postId)}/events`

  ;(async () => {
    try {
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          Accept: 'text/event-stream',
          Authorization: `Bearer ${accessToken}`,
        },
        credentials: 'include',
        signal: controller.signal,
      })
      if (!response.ok || !response.body) {
        handlers.onError?.(new Error(`SSE failed (${response.status})`))
        return
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      let eventName = 'message'
      let dataLines = []

      const flush = () => {
        if (dataLines.length === 0) {
          eventName = 'message'
          return
        }
        const raw = dataLines.join('\n')
        dataLines = []
        const name = eventName
        eventName = 'message'
        let payload = raw
        try {
          payload = JSON.parse(raw)
        } catch {
          // keep string payload
        }
        if (name === 'like') handlers.onLike?.(payload)
        else if (name === 'comment_created') handlers.onCommentCreated?.(payload)
        else if (name === 'comment_deleted') handlers.onCommentDeleted?.(payload)
        else if (name === 'connected') handlers.onConnected?.(payload)
      }

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const parts = buffer.split(/\r?\n/)
        buffer = parts.pop() ?? ''
        for (const line of parts) {
          if (line === '') {
            flush()
            continue
          }
          if (line.startsWith(':')) continue
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim()
            continue
          }
          if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).trimStart())
          }
        }
      }
      flush()
    } catch (err) {
      if (err?.name === 'AbortError') return
      handlers.onError?.(err)
    }
  })()

  return () => controller.abort()
}
