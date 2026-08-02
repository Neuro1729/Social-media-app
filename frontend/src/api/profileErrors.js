/**
 * Map profile/search HTTP failures to user-facing copy.
 * Prefers a safe backend `error`/`message` when present.
 */
export function profileLoadError(err) {
  const status = err?.response?.status
  const backend = err?.response?.data?.error || err?.response?.data?.message

  if (!err?.response || status === 502) {
    return backend || 'Backend is temporarily unavailable'
  }
  if (status === 401) {
    return backend || 'Session expired; log in again'
  }
  if (status === 403) {
    return backend || 'Access denied'
  }
  if (status === 404) {
    return backend || 'User not found or profile unavailable'
  }
  if (status >= 500) {
    return backend || 'Server could not load the profile'
  }
  return backend || 'User not found or profile unavailable'
}
