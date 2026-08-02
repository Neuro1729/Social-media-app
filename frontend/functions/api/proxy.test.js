import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildForwardHeaders,
  buildProxyResponse,
  buildTargetUrl,
  resolveBackendOrigin,
} from './_helpers.js'

test('resolveBackendOrigin rejects missing value', () => {
  const result = resolveBackendOrigin('', { requireHttps: true })
  assert.equal(result.status, 500)
  assert.match(result.error, /not configured/)
})

test('resolveBackendOrigin strips trailing slash and requires https', () => {
  const ok = resolveBackendOrigin('https://api.example.com/', { requireHttps: true })
  assert.equal(ok.origin, 'https://api.example.com')

  const httpDenied = resolveBackendOrigin('http://localhost:8080', { requireHttps: true })
  assert.equal(httpDenied.status, 500)

  const httpAllowed = resolveBackendOrigin('http://localhost:8080', { requireHttps: false })
  assert.equal(httpAllowed.origin, 'http://localhost:8080')
})

test('buildTargetUrl forwards path and query', () => {
  const url = buildTargetUrl(
    'https://api.example.com',
    'https://pages.example/api/auth/refresh?x=1',
    ['auth', 'refresh'],
  )
  assert.equal(url, 'https://api.example.com/api/auth/refresh?x=1')
})

test('buildForwardHeaders keeps auth/cookie and drops host', () => {
  const incoming = new Headers({
    Authorization: 'Bearer abc',
    Cookie: 'refreshToken=secret',
    Host: 'pages.example',
    'Content-Type': 'application/json',
    'X-Request-Id': 'r1',
  })
  const forwarded = buildForwardHeaders(incoming)
  assert.equal(forwarded.get('Authorization'), 'Bearer abc')
  assert.equal(forwarded.get('Cookie'), 'refreshToken=secret')
  assert.equal(forwarded.get('Content-Type'), 'application/json')
  assert.equal(forwarded.get('X-Request-Id'), 'r1')
  assert.equal(forwarded.get('Host'), null)
})

test('buildProxyResponse preserves status and Set-Cookie', async () => {
  const backend = new Response(JSON.stringify({ ok: true }), {
    status: 201,
    headers: {
      'Content-Type': 'application/json',
      'Set-Cookie': 'refreshToken=abc; Path=/api/auth; HttpOnly; SameSite=Lax',
    },
  })
  const proxied = buildProxyResponse(backend)
  assert.equal(proxied.status, 201)
  assert.match(proxied.headers.get('set-cookie') || '', /refreshToken=abc/)
  assert.equal(await proxied.json().then((b) => b.ok), true)
})
