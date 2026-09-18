import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createTodo, deleteTodo, listTodos, updateTodo } from './index'

// 本项目的 api 层只用了 status/ok/json()，用最简对象模拟 fetch 返回即可。
// 统一请求层会用 status 判定 204、用 ok 判定是否抛 ApiError。
function mockFetch(body?: unknown, status = 200) {
  return vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  })
}

beforeEach(() => {
  // 统一请求层对非安全方法会读取 XSRF-TOKEN；预置好可跳过 /csrf 拉取
  Object.defineProperty(document, 'cookie', {
    configurable: true,
    value: 'XSRF-TOKEN=test-token',
  })
})

afterEach(() => {
  vi.unstubAllGlobals()
})

/** 统一请求层固定带的公共请求参数 */
const sharedInit = {
  credentials: 'same-origin',
  headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
}

describe('listTodos', () => {
  it('向 /api/todos 发 GET，解析返回的列表', async () => {
    const fetchMock = mockFetch([{ id: 1, title: '买牛奶', done: false }])
    vi.stubGlobal('fetch', fetchMock)

    const todos = await listTodos()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos',
      expect.objectContaining({ method: 'GET', ...sharedInit }),
    )
    expect(todos).toEqual([{ id: 1, title: '买牛奶', done: false }])
  })
})

describe('createTodo', () => {
  it('向 /api/todos 发 POST，请求体只有 title，返回新建的待办', async () => {
    const fetchMock = mockFetch({ id: 2, title: '写周报', done: false })
    vi.stubGlobal('fetch', fetchMock)

    const created = await createTodo('写周报')

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ title: '写周报' }),
        ...sharedInit,
      }),
    )
    expect(created).toEqual({ id: 2, title: '写周报', done: false })
  })
})

describe('updateTodo', () => {
  it('向 /api/todos/{id} 发 PUT，请求体带 title 和 done', async () => {
    const fetchMock = mockFetch({ id: 3, title: '学习', done: true })
    vi.stubGlobal('fetch', fetchMock)

    const updated = await updateTodo({ id: 3, title: '学习', done: true })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos/3',
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify({ title: '学习', done: true }),
        ...sharedInit,
      }),
    )
    expect(updated).toEqual({ id: 3, title: '学习', done: true })
  })
})

describe('deleteTodo', () => {
  it('向 /api/todos/{id} 发 DELETE', async () => {
    const fetchMock = mockFetch(undefined, 204)
    vi.stubGlobal('fetch', fetchMock)

    await deleteTodo(3)

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos/3',
      expect.objectContaining({ method: 'DELETE', ...sharedInit }),
    )
  })
})

describe('认证过期', () => {
  it('propagates unauthorized ApiError from the Todo API', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({ code: 'AUTH_REQUIRED', message: '未登录' }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(listTodos()).rejects.toMatchObject({
      status: 401,
      code: 'AUTH_REQUIRED',
      message: '未登录',
    })
  })
})
