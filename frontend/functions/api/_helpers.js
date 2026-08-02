/**
 * Pure helpers for the Cloudflare Pages /api proxy.
 * Safe to unit-test without a live backend.
 */

const HOP_BY_HOP = new Set([
  'connection',
  'keep-alive',
  'proxy-authenticate',
  'proxy-authorization',
  'te',
  'trailers',
  'transfer-encoding',
  'upgrade',
  'host',
  'content-length',
])

const FORWARD_REQUEST_HEADERS = new Set([
  'authorization',
  'content-type',
  'accept',
  'cookie',
  'accept-language',
  'if-none-match',
  'if-modified-since',
])

/**
 * @param {string | undefined} raw
 * @param {{ requireHttps?: boolean }} [options]
 */
export function resolveBackendOrigin(raw, options = {}) {
  if (raw == null || String(raw).trim() === '') {
    return { error: 'BACKEND_ORIGIN is not configured', status: 500 }
  }
  let origin = String(raw).trim().replace(/\/+$/, '')
  let parsed
  try {
    parsed = new URL(origin)
  } catch {
    return { error: 'BACKEND_ORIGIN must be an absolute URL', status: 500 }
  }
  if (parsed.pathname !== '/' || parsed.search || parsed.hash) {
    origin = `${parsed.protocol}//${parsed.host}`
  }
  const requireHttps = options.requireHttps !== false
  if (requireHttps && parsed.protocol !== 'https:') {
    return { error: 'BACKEND_ORIGIN must be an absolute HTTPS origin', status: 500 }
  }
  if (!requireHttps && parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
    return { error: 'BACKEND_ORIGIN must be an absolute HTTP(S) origin', status: 500 }
  }
  return { origin }
}

/**
 * @param {string} backendOrigin
 * @param {string} requestUrl
 * @param {string[]} [pathParams]
 */
export function buildTargetUrl(backendOrigin, requestUrl, pathParams) {
  const incoming = new URL(requestUrl)
  const suffix = Array.isArray(pathParams) && pathParams.length > 0
    ? pathParams.map(encodeURIComponent).join('/')
    : ''
  const path = suffix ? `/api/${suffix}` : '/api'
  const target = new URL(path, `${backendOrigin}/`)
  target.search = incoming.search
  return target.toString()
}

/**
 * @param {Headers} incoming
 */
export function buildForwardHeaders(incoming) {
  const headers = new Headers()
  for (const [key, value] of incoming.entries()) {
    const lower = key.toLowerCase()
    if (HOP_BY_HOP.has(lower)) continue
    if (FORWARD_REQUEST_HEADERS.has(lower) || lower.startsWith('x-')) {
      headers.set(key, value)
    }
  }
  return headers
}

/**
 * @param {Response} backendResponse
 */
export function buildProxyResponse(backendResponse) {
  const headers = new Headers()
  backendResponse.headers.forEach((value, key) => {
    const lower = key.toLowerCase()
    if (HOP_BY_HOP.has(lower)) return
    if (lower === 'set-cookie') return
    headers.set(key, value)
  })

  const response = new Response(backendResponse.body, {
    status: backendResponse.status,
    statusText: backendResponse.statusText,
    headers,
  })

  const getSetCookie = backendResponse.headers.getSetCookie?.bind(backendResponse.headers)
  const cookies = typeof getSetCookie === 'function' ? getSetCookie() : null
  if (cookies && cookies.length > 0) {
    for (const cookie of cookies) {
      response.headers.append('Set-Cookie', cookie)
    }
  } else {
    const single = backendResponse.headers.get('set-cookie')
    if (single) {
      response.headers.append('Set-Cookie', single)
    }
  }

  return response
}

export function jsonError(status, message) {
  return new Response(JSON.stringify({ error: message }), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
