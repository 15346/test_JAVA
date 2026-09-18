// ============================================================
//  接口层：数据长什么样在 ./type.ts，这里只管"怎么调"。
//  开发时 /api/todos 由 Vite 代理到 http://localhost:18081；
//  构建后由 Spring Boot 在同源 18081 下直接提供服务。
// ============================================================

import { request } from '../../../shared/http/request'
import type { Todo } from './type'

const API = '/api/todos'

/** 获取全部待办 —— GET */
export function listTodos(): Promise<Todo[]> {
  return request<Todo[]>(API, { method: 'GET' })
}

/** 新增待办 —— POST，请求体 {"title":"xxx"} */
export function createTodo(title: string): Promise<Todo> {
  return request<Todo>(API, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title }),
  })
}

/** 更新待办 —— PUT，请求体 {"title":"xxx","done":true} */
export function updateTodo(todo: Todo): Promise<Todo> {
  return request<Todo>(`${API}/${todo.id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title: todo.title, done: todo.done }),
  })
}

/** 删除待办 —— DELETE（204 无响应体，request<void> 返回 undefined） */
export function deleteTodo(id: number): Promise<void> {
  return request<void>(`${API}/${id}`, { method: 'DELETE' })
}
