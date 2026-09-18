import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getCurrentUser, login, logout, register, resetPassword } from './index'

function jsonResponse(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}

function emptyResponse(): Response {
  return new Response(null, { status: 204 })
}

beforeEach(() => {
  Object.defineProperty(document, 'cookie', {
    configurable: true,
    value: 'XSRF-TOKEN=test-token',
  })
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('register', () => {
  it('sends email and both passwords', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse({ id: 1, email: 'hello@126.com', role: 'USER' }))
    vi.stubGlobal('fetch', fetchMock)

    await register({ email: 'Hello@126.COM', password: 'abc12345', confirmPassword: 'abc12345' })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/register',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          email: 'Hello@126.COM',
          password: 'abc12345',
          confirmPassword: 'abc12345',
        }),
      }),
    )
  })
})

describe('auth endpoints', () => {
  it('login, me, logout and reset use the agreed endpoints', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ id: 1, email: 'hello@126.com', role: 'USER' }))
      .mockResolvedValueOnce(jsonResponse({ id: 1, email: 'hello@126.com', role: 'USER' }))
      .mockResolvedValueOnce(emptyResponse())
      .mockResolvedValueOnce(emptyResponse())
    vi.stubGlobal('fetch', fetchMock)

    await login({ email: 'hello@126.com', password: 'abc12345' })
    await getCurrentUser()
    await logout()
    await resetPassword({
      email: 'hello@126.com',
      code: '123456',
      // ResetPasswordPayload extends RegisterPayload，新密码字段即继承的 password
      password: 'new12345',
      confirmPassword: 'new12345',
    })

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      '/api/auth/login',
      '/api/auth/me',
      '/api/auth/logout',
      '/api/auth/password/reset',
    ])

    // 覆盖 password -> 接口字段 newPassword 的映射
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/password/reset',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          email: 'hello@126.com',
          code: '123456',
          newPassword: 'new12345',
          confirmPassword: 'new12345',
        }),
      }),
    )
  })
})

describe('requestResetCode', () => {
  it('posts the email to the reset-code endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(emptyResponse())
    vi.stubGlobal('fetch', fetchMock)

    const { requestResetCode } = await import('./index')
    await requestResetCode('hello@126.com')

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/password/reset-code',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ email: 'hello@126.com' }),
      }),
    )
  })
})
