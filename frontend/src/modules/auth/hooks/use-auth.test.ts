import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../shared/http/request'
import { getCurrentUser, logout } from '../api'
import { useAuth } from './use-auth'

vi.mock('../api', () => ({
  getCurrentUser: vi.fn(),
  logout: vi.fn(),
}))

const getCurrentUserMock = vi.mocked(getCurrentUser)

beforeEach(() => {
  getCurrentUserMock.mockReset()
  useAuth().clearCurrentUser()
})

describe('useAuth', () => {
  it('loads and stores current user', async () => {
    getCurrentUserMock.mockResolvedValue({ id: 1, email: 'a@example.com', role: 'USER' })

    const auth = useAuth()
    await expect(auth.loadCurrentUser()).resolves.toEqual({
      id: 1, email: 'a@example.com', role: 'USER',
    })
    expect(auth.currentUser.value?.email).toBe('a@example.com')
  })

  it('clears current user when me returns 401', async () => {
    getCurrentUserMock.mockRejectedValue(new ApiError(401, 'AUTH_REQUIRED', '未登录'))

    const auth = useAuth()
    await expect(auth.loadCurrentUser()).resolves.toBeNull()
    expect(auth.currentUser.value).toBeNull()
  })
})
