import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, request } from './request'

/** jsdom 允许通过 defineProperty 覆盖 document.cookie 的读取值 */
function setCookie(value: string) {
  Object.defineProperty(document, 'cookie', {
    configurable: true,
    value,
  })
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

beforeEach(() => {
  setCookie('')
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('request', () => {
  it('adds JSON header, same-origin credentials and parses JSON', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: 1 }))
    vi.stubGlobal('fetch', fetchMock)

    const result = await request<{ id: number }>('/api/test', {
      method: 'POST',
      body: JSON.stringify({ value: 1 }),
    })

    expect(result).toEqual({ id: 1 })
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/test',
      expect.objectContaining({
        credentials: 'same-origin',
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
      }),
    )
  })

  it('throws ApiError using backend code and message', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({ code: 'EMAIL_ALREADY_EXISTS', message: '邮箱不可用' }, 409),
      ),
    )

    await expect(request('/api/auth/register', { method: 'POST' })).rejects.toMatchObject({
      status: 409,
      code: 'EMAIL_ALREADY_EXISTS',
      message: '邮箱不可用',
    })
  })

  it('returns undefined for a 204 response', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })))

    const result = await request<void>('/api/auth/logout', { method: 'POST' })

    expect(result).toBeUndefined()
  })

  it('sends X-XSRF-TOKEN for unsafe methods when the cookie is present', async () => {
    setCookie('XSRF-TOKEN=csrf-abc')
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: 1 }))
    vi.stubGlobal('fetch', fetchMock)

    await request('/api/auth/login', { method: 'POST', body: '{}' })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/login',
      expect.objectContaining({
        headers: expect.objectContaining({ 'X-XSRF-TOKEN': 'csrf-abc' }),
      }),
    )
  })

  it('primes the CSRF cookie via GET /api/auth/csrf once when it is absent', async () => {
    setCookie('')
    vi.resetModules()
    const { request: freshRequest } = await import('./request')
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: 1 }))
    vi.stubGlobal('fetch', fetchMock)

    await freshRequest('/api/auth/login', { method: 'POST', body: '{}' })

    const urls = fetchMock.mock.calls.map(([url]) => url)
    expect(urls.filter((url) => url === '/api/auth/csrf')).toHaveLength(1)
  })

  it('exposes ApiError as a named class', () => {
    const error = new ApiError(400, 'BAD_REQUEST', '坏了')

    expect(error.name).toBe('ApiError')
    expect(error.status).toBe(400)
    expect(error.code).toBe('BAD_REQUEST')
    expect(error.message).toBe('坏了')
  })
})
