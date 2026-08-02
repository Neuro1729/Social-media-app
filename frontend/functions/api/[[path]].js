/**
 * Cloudflare Pages Function: same-origin /api/* proxy to the backend origin.
 *
 * Configure in Cloudflare Pages → Settings → Variables and Secrets:
 *   BACKEND_ORIGIN=https://your-backend.example.com
 *
 * Leave VITE_API_ORIGIN empty for this deployment (browser calls same-origin /api).
 */
import {
  buildForwardHeaders,
  buildProxyResponse,
  buildTargetUrl,
  jsonError,
  resolveBackendOrigin,
} from './_helpers.js'

/**
 * @param {EventContext} context
 */
export async function onRequest(context) {
  const requireHttps = !(context.env?.ALLOW_INSECURE_BACKEND_ORIGIN === 'true')
  const resolved = resolveBackendOrigin(context.env?.BACKEND_ORIGIN, { requireHttps })
  if (resolved.error) {
    return jsonError(resolved.status, resolved.error)
  }

  const pathParams = context.params?.path
  const segments = Array.isArray(pathParams)
    ? pathParams
    : (pathParams ? [pathParams] : [])
  const targetUrl = buildTargetUrl(resolved.origin, context.request.url, segments)

  const incomingHost = new URL(context.request.url).host
  const targetHost = new URL(targetUrl).host
  if (incomingHost === targetHost) {
    return jsonError(500, 'BACKEND_ORIGIN must not point at this Pages host')
  }

  const method = context.request.method.toUpperCase()
  const headers = buildForwardHeaders(context.request.headers)
  const init = { method, headers, redirect: 'manual' }
  if (method !== 'GET' && method !== 'HEAD') {
    init.body = context.request.body
    init.duplex = 'half'
  }

  let backendResponse
  try {
    backendResponse = await fetch(targetUrl, init)
  } catch {
    return jsonError(502, 'Backend is temporarily unavailable')
  }

  return buildProxyResponse(backendResponse)
}
