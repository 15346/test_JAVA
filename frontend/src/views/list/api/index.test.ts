import { afterEach, describe, expect, it, vi } from 'vitest'
import { createTodo, deleteTodo, listTodos, updateTodo } from './index'

// 本项目的 api 层只用了 res.json()，用最简对象模拟 fetch 返回即可
function mockFetch(body?: unknown) {
  return vi.fn().mockResolvedValue({ json: async () => body })
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('listTodos', () => {
  it('向 /api/todos 发 GET，解析返回的列表', async () => {
    const fetchMock = mockFetch([{ id: 1, title: '买牛奶', done: false }])
    vi.stubGlobal('fetch', fetchMock)

    const todos = await listTodos()

    expect(fetchMock).toHaveBeenCalledWith('/api/todos', { method: 'GET' })
    expect(todos).toEqual([{ id: 1, title: '买牛奶', done: false }])
  })
})

describe('createTodo', () => {
  it('向 /api/todos 发 POST，请求体只有 title，返回新建的待办', async () => {
    const fetchMock = mockFetch({ id: 2, title: '写周报', done: false })
    vi.stubGlobal('fetch', fetchMock)

    const created = await createTodo('写周报')

    expect(fetchMock).toHaveBeenCalledWith('/api/todos', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title: '写周报' }),
    })
    expect(created).toEqual({ id: 2, title: '写周报', done: false })
  })
})

describe('updateTodo', () => {
  it('向 /api/todos/{id} 发 PUT，请求体带 title 和 done', async () => {
    const fetchMock = mockFetch({ id: 3, title: '学习', done: true })
    vi.stubGlobal('fetch', fetchMock)

    const updated = await updateTodo({ id: 3, title: '学习', done: true })

    expect(fetchMock).toHaveBeenCalledWith('/api/todos/3', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title: '学习', done: true }),
    })
    expect(updated).toEqual({ id: 3, title: '学习', done: true })
  })
})

describe('deleteTodo', () => {
  it('向 /api/todos/{id} 发 DELETE', async () => {
    const fetchMock = mockFetch(undefined)
    vi.stubGlobal('fetch', fetchMock)

    await deleteTodo(3)

    expect(fetchMock).toHaveBeenCalledWith('/api/todos/3', { method: 'DELETE' })
  })
})
