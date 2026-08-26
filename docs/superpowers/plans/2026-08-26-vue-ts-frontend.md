# 前端改造 Vue 3 + TypeScript（中后台结构）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `frontend/` 替换为 Vite + Vue 3 + TS + ant-design-vue 的中后台模块结构应用（api/type 分离、constant、权限 hooks、views 组装层、use-search/use-table-columns 配置化 hooks、抽屉、hash 路由双页面），后端零改动。

**Architecture:** `frontend/src/` 即业务模块：`views/list/index.vue` 组装搜索栏 + 表格 + 抽屉；搜索/分页前端本地做；构建产物输出 `src/main/resources/static` 由 Spring Boot 8081 托管，开发走 5173 + Vite 代理。

**Tech Stack:** Vue ^3.5、ant-design-vue ^4.2（按需引入）、vue-router ^4.5（hash）、@vitejs/plugin-vue-jsx、unplugin-vue-components、vitest + jsdom。

**Spec:** `docs/superpowers/specs/2026-08-26-vue-ts-frontend-design.md`

## Global Constraints

- 后端代码**零改动**（含 CorsConfig、application.properties、接口）。
- 后端端口 **8081**，Vite 开发端口 **5173**，`/api` 代理到 `http://localhost:8081`。
- 接口契约：`GET/POST /api/todos`、`PUT/DELETE /api/todos/{id}`，`Todo { id: number; title: string; done: boolean }`。
- antd **按需引入**（unplugin-vue-components，仅 SFC 模板生效；`.tsx` 内组件显式 import）。
- 路由用 **hash 模式**；**不引入** Pinia、axios、ESLint。
- `npm run build` 产物输出到 `src/main/resources/static`，该目录**保持 git 跟踪**。
- 所有 shell 命令在项目根执行；前端命令每次调用都要 `cd frontend`（Bash 调用间不共享 cwd）。

---

### Task 1: Vite + Vue 3 + TS + JSX + antd 按需构建链骨架

**Files:**
- Delete: `frontend/index.html`、`frontend/app.js`、`frontend/style.css`
- Create: `frontend/package.json`、`frontend/vite.config.ts`、`frontend/tsconfig.json`、`frontend/index.html`、`frontend/src/main.ts`、`frontend/src/App.vue`（占位）
- Modify: `.gitignore`

**Interfaces:**
- Consumes: 无
- Produces: 可 dev/build/test 的空壳；`npm run test` 脚本；构建插件链（后续任务直接用）。

- [ ] **Step 1: 删除旧前端三件**

```bash
git rm frontend/index.html frontend/app.js frontend/style.css
```

- [ ] **Step 2: 创建 `frontend/package.json`**

版本若 npm 解析失败，用 `npm install <pkg>@latest` 对应替换后继续。

```json
{
  "name": "todo-frontend",
  "version": "0.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc --noEmit && vite build",
    "test": "vitest run",
    "test:watch": "vitest"
  },
  "dependencies": {
    "ant-design-vue": "^4.2.6",
    "vue": "^3.5.13",
    "vue-router": "^4.5.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^6.0.0",
    "@vitejs/plugin-vue-jsx": "^4.1.0",
    "jsdom": "^26.1.0",
    "typescript": "~5.8.3",
    "unplugin-vue-components": "^28.0.0",
    "vite": "^7.0.0",
    "vitest": "^3.2.4",
    "vue-tsc": "^2.2.12"
  }
}
```

- [ ] **Step 3: 创建 `frontend/vite.config.ts`**

```ts
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    // antd 组件按需自动引入：只在 SFC 模板里生效（<a-button> 等）；
    // .tsx 里的 JSX 不走模板编译，组件需显式 import。
    // v4 用 cssinjs，无需引入样式文件。
    Components({
      resolvers: [AntDesignVueResolver({ importStyle: false })],
    }),
  ],
  // 开发时：npm run dev 起在 5173，/api 请求代理给后端 8081
  server: {
    proxy: {
      '/api': 'http://localhost:8081',
    },
  },
  // 构建：产物输出到 Spring Boot 静态资源目录
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
  // 单测：jsdom 环境
  test: {
    environment: 'jsdom',
  },
})
```

- [ ] **Step 4: 创建 `frontend/tsconfig.json`**

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "jsx": "preserve",
    "strict": true,
    "noEmit": true,
    "isolatedModules": true,
    "skipLibCheck": true,
    "types": ["vite/client"]
  },
  "include": ["src/**/*.ts", "src/**/*.tsx", "src/**/*.d.ts", "src/**/*.vue", "vite.config.ts"]
}
```

- [ ] **Step 5: 创建 `frontend/index.html`**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>待办管理 · 前后端分离示例</title>
    <link rel="icon" href="data:,">
</head>
<body>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
</body>
</html>
```

- [ ] **Step 6: 创建 `frontend/src/main.ts`**

```ts
import { createApp } from 'vue'
import App from './App.vue'

// 把根组件挂到 index.html 的 #app 上
// （Task 3 接入 router 后会变成 createApp(App).use(router).mount('#app')）
createApp(App).mount('#app')
```

- [ ] **Step 7: 创建占位 `frontend/src/App.vue`**

```vue
<script setup lang="ts">
</script>

<template>
  <div style="max-width: 720px; margin: 80px auto; text-align: center">
    <h1>📝 待办管理（搭建中）</h1>
    <p>Spring Boot 后端 + Vue 3 + TypeScript + ant-design-vue 前端</p>
  </div>
</template>
```

- [ ] **Step 8: `.gitignore` 末尾追加**

```
### Node / 前端 ###
node_modules/
```

- [ ] **Step 9: 安装依赖并验证构建**

```bash
cd frontend && npm install && npm run build
```

Expected: 安装成功；`vue-tsc --noEmit` 无报错；vite `✓ built in ...`；`ls ../src/main/resources/static` 可见 `index.html` 与 `assets/`。

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat: 前端替换为 Vite + Vue3 + TS + antd 按需构建链骨架"
```

---

### Task 2: api 层（type.ts + index.ts，TDD）

**Files:**
- Create: `frontend/src/api/type.ts`、`frontend/src/api/index.ts`
- Test: `frontend/src/api/index.test.ts`

**Interfaces:**
- Consumes: 无
- Produces（后续所有任务依赖，签名必须一致）:
  - `api/type.ts`: `export interface Todo { id: number; title: string; done: boolean }`
  - `api/index.ts`: `listTodos(): Promise<Todo[]>`、`createTodo(title: string): Promise<Todo>`、`updateTodo(todo: Todo): Promise<Todo>`、`deleteTodo(id: number): Promise<void>`

- [ ] **Step 1: 写失败测试 `frontend/src/api/index.test.ts`**

```ts
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
```

- [ ] **Step 2: 跑测试确认失败**

```bash
cd frontend && npm run test
```

Expected: FAIL，`Failed to resolve import "./index"`。

- [ ] **Step 3: 写 `frontend/src/api/type.ts`**

```ts
/** 一条待办 —— 对应后端 com.example.demo.entity.Todo */
export interface Todo {
  id: number
  title: string
  done: boolean
}
```

- [ ] **Step 4: 写 `frontend/src/api/index.ts`**

```ts
// ============================================================
//  接口层：数据长什么样在 ./type.ts，这里只管"怎么调"。
//  开发时 /api/todos 由 Vite 代理到 http://localhost:8081；
//  构建后由 Spring Boot 在同源 8081 下直接提供服务。
// ============================================================

import type { Todo } from './type'

const API = '/api/todos'

/** 统一发请求 + 解析 JSON */
async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init)
  return res.json() as Promise<T>
}

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

/** 删除待办 —— DELETE（无响应体） */
export async function deleteTodo(id: number): Promise<void> {
  await fetch(`${API}/${id}`, { method: 'DELETE' })
}
```

- [ ] **Step 5: 跑测试确认通过**

```bash
cd frontend && npm run test
```

Expected: 4 个 PASS。

- [ ] **Step 6: Commit**

```bash
git add frontend/src/api
git commit -m "feat: 前端接口层（type.ts 类型 + index.ts 函数）及单测"
```

---

### Task 3: constant、权限配置、路由与页面外壳

**Files:**
- Create: `frontend/src/constant/index.ts`、`frontend/src/use-permission-config.ts`、`frontend/src/router/index.ts`、`frontend/src/views/list/index.vue`（占位）、`frontend/src/views/operation-log/index.vue`
- Test: `frontend/src/use-permission-config.test.ts`
- Modify: `frontend/src/main.ts`、`frontend/src/App.vue`

**Interfaces:**
- Consumes: 无新依赖（路由懒加载引用占位页面）
- Produces:
  - `constant/index.ts`: `STATUS_OPTIONS: { label: string; value: string }[]`（全部/未完成/已完成）
  - `use-permission-config.ts`: `usePermissionConfig(): PermissionConfig`，`PermissionConfig { canAdd: boolean; canEdit: boolean; canDelete: boolean }`
  - `router/index.ts`: 默认导出 router（hash 模式，`/` 重定向 `/list`）

- [ ] **Step 1: 写失败测试 `frontend/src/use-permission-config.test.ts`**

```ts
import { describe, expect, it } from 'vitest'
import { usePermissionConfig } from './use-permission-config'

describe('usePermissionConfig', () => {
  it('返回新增/编辑/删除三个权限点', () => {
    const p = usePermissionConfig()
    expect(p).toEqual({ canAdd: true, canEdit: true, canDelete: true })
  })
})
```

- [ ] **Step 2: 跑测试确认失败**

```bash
cd frontend && npm run test
```

Expected: 新用例 FAIL（模块不存在），api 的 4 个仍 PASS。

- [ ] **Step 3: 写 `frontend/src/constant/index.ts`**

```ts
/** 固定值集中放这里：下拉选项、字典等 */

/** 列表页「状态」下拉筛选项（本地过滤用） */
export interface StatusOption {
  label: string
  value: string
}

export const STATUS_OPTIONS: StatusOption[] = [
  { label: '全部', value: 'all' },
  { label: '未完成', value: 'undone' },
  { label: '已完成', value: 'done' },
]
```

- [ ] **Step 4: 写 `frontend/src/use-permission-config.ts`**

```ts
/**
 * 按钮权限配置。
 * 真实项目里这里通常读当前用户角色或接口下发的权限点；
 * 本示例没有登录体系，静态全开，演示「视图层按权限点控制按钮显隐」的写法。
 */
export interface PermissionConfig {
  canAdd: boolean
  canEdit: boolean
  canDelete: boolean
}

export function usePermissionConfig(): PermissionConfig {
  return {
    canAdd: true,
    canEdit: true,
    canDelete: true,
  }
}
```

- [ ] **Step 5: 写 `frontend/src/router/index.ts`**

```ts
import { createRouter, createWebHashHistory } from 'vue-router'

/**
 * hash 路由（地址带 #）：构建产物由 Spring Boot 静态托管，
 * 深链刷新（如直接打开 /#/operation-log）不需要后端配 SPA 转发。
 */
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/list' },
    {
      path: '/list',
      name: 'TodoList',
      component: () => import('../views/list/index.vue'),
    },
    {
      path: '/operation-log',
      name: 'OperationLog',
      component: () => import('../views/operation-log/index.vue'),
    },
  ],
})

export default router
```

- [ ] **Step 6: 写占位 `frontend/src/views/list/index.vue`**

```vue
<script setup lang="ts">
</script>

<template>
  <a-card>
    <a-empty description="待办列表（Task 5 实现完整功能）" />
  </a-card>
</template>
```

- [ ] **Step 7: 写 `frontend/src/views/operation-log/index.vue`（空壳）**

```vue
<script setup lang="ts">
</script>

<template>
  <a-card>
    <a-empty description="操作日志（占位页，待实现）" />
  </a-card>
</template>
```

- [ ] **Step 8: 替换 `frontend/src/main.ts` 接入路由**

```ts
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

createApp(App).use(router).mount('#app')
```

- [ ] **Step 9: 替换 `frontend/src/App.vue` 为布局外壳**

```vue
<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

// 侧边导航（真实项目一般由菜单接口/权限生成）
const menus = [
  { key: '/list', label: '待办列表' },
  { key: '/operation-log', label: '操作日志' },
]
</script>

<template>
  <a-layout style="min-height: 100vh">
    <a-layout-header style="background: #001529; display: flex; align-items: center">
      <div style="color: #fff; font-size: 18px; font-weight: 600">📝 待办管理</div>
    </a-layout-header>
    <a-layout>
      <a-layout-sider theme="light">
        <a-menu mode="inline" :selected-keys="[route.path]">
          <a-menu-item v-for="m in menus" :key="m.key">
            <RouterLink :to="m.key">{{ m.label }}</RouterLink>
          </a-menu-item>
        </a-menu>
      </a-layout-sider>
      <a-layout-content style="padding: 24px">
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>
```

- [ ] **Step 10: 验证测试与构建**

```bash
cd frontend && npm run test && npm run build
```

Expected: 5 个测试 PASS；构建成功（懒加载分包正常生成）。

- [ ] **Step 11: Commit**

```bash
git add frontend/src
git commit -m "feat: constant/权限配置/hash 路由与布局外壳、操作日志空壳页"
```

---

### Task 4: use-search.tsx 与 use-table-columns.tsx（TDD）

**Files:**
- Create: `frontend/src/views/list/hooks/use-search.tsx`、`frontend/src/views/list/hooks/use-table-columns.tsx`
- Test: `frontend/src/views/list/hooks/use-search.test.ts`、`frontend/src/views/list/hooks/use-table-columns.test.ts`

**Interfaces:**
- Consumes: `Todo`（api/type）、`STATUS_OPTIONS`（constant）、`PermissionConfig`（use-permission-config）
- Produces（Task 5 依赖）:
  - `useSearch(): { model: SearchModel; fields: SearchField[]; reset: () => void }`，`SearchModel { keyword: string; status: string }`
  - `useTableColumns(options: { onEdit: (t: Todo) => void; onDelete: (t: Todo) => void; onToggle: (t: Todo) => void; permission: PermissionConfig }): TableColumnType<Todo>[]`

- [ ] **Step 1: 写失败测试 `frontend/src/views/list/hooks/use-search.test.ts`**

```ts
import { describe, expect, it } from 'vitest'
import { useSearch } from './use-search'

describe('useSearch', () => {
  it('初始模型为空关键词 + 全部状态', () => {
    const { model } = useSearch()
    expect(model.keyword).toBe('')
    expect(model.status).toBe('all')
  })

  it('字段配置覆盖关键词与状态', () => {
    const { fields } = useSearch()
    expect(fields.map((f) => f.key)).toEqual(['keyword', 'status'])
  })

  it('reset 清空搜索条件', () => {
    const { model, reset } = useSearch()
    model.keyword = '牛奶'
    model.status = 'done'
    reset()
    expect(model.keyword).toBe('')
    expect(model.status).toBe('all')
  })
})
```

- [ ] **Step 2: 写失败测试 `frontend/src/views/list/hooks/use-table-columns.test.ts`**

```ts
import { describe, expect, it, vi } from 'vitest'
import { useTableColumns } from './use-table-columns'
import type { PermissionConfig } from '../../../use-permission-config'

const permission: PermissionConfig = { canAdd: true, canEdit: true, canDelete: true }

function makeColumns(p: PermissionConfig = permission) {
  return useTableColumns({
    onEdit: vi.fn(),
    onDelete: vi.fn(),
    onToggle: vi.fn(),
    permission: p,
  })
}

describe('useTableColumns', () => {
  it('生成 ID/标题/状态/操作 四列', () => {
    const keys = makeColumns().map((c) => String(c.key ?? c.dataIndex))
    expect(keys).toEqual(['id', 'title', 'done', 'action'])
  })

  it('状态列和操作列提供 customRender（JSX 渲染）', () => {
    const columns = makeColumns()
    expect(typeof columns[2].customRender).toBe('function')
    expect(typeof columns[3].customRender).toBe('function')
  })

  it('权限关闭时列结构不变（按钮显隐在渲染层处理）', () => {
    const columns = makeColumns({ canAdd: true, canEdit: false, canDelete: false })
    expect(columns).toHaveLength(4)
  })
})
```

- [ ] **Step 3: 跑测试确认失败**

```bash
cd frontend && npm run test
```

Expected: 6 个新用例 FAIL（两个 hooks 模块不存在），已有 5 个仍 PASS。

- [ ] **Step 4: 写 `frontend/src/views/list/hooks/use-search.tsx`**

```tsx
import { reactive } from 'vue'
import { STATUS_OPTIONS } from '../../../constant'

/**
 * 搜索栏配置。index.vue 按 fields 渲染表单项；
 * .tsx 后缀：将来某个搜索项需要自定义渲染时直接写 JSX。
 */
export interface SearchModel {
  keyword: string
  status: string
}

export interface SearchField {
  label: string
  key: keyof SearchModel
  component: 'input' | 'select'
  placeholder?: string
  options?: { label: string; value: string }[]
}

export function useSearch() {
  const model = reactive<SearchModel>({ keyword: '', status: 'all' })

  const fields: SearchField[] = [
    {
      label: '关键词',
      key: 'keyword',
      component: 'input',
      placeholder: '按标题搜索',
    },
    {
      label: '状态',
      key: 'status',
      component: 'select',
      options: STATUS_OPTIONS,
    },
  ]

  function reset() {
    model.keyword = ''
    model.status = 'all'
  }

  return { model, fields, reset }
}
```

- [ ] **Step 5: 写 `frontend/src/views/list/hooks/use-table-columns.tsx`**

```tsx
import { Button, Popconfirm, Switch } from 'ant-design-vue'
import type { TableColumnType } from 'ant-design-vue'
import type { Todo } from '../../../api/type'
import type { PermissionConfig } from '../../../use-permission-config'

/**
 * 表格列配置。.tsx：状态列/操作列的 customRender 用 JSX 写最顺手。
 * 注意：JSX 不经过模板编译，antd 组件必须显式 import（按需插件只作用于模板）。
 */
export interface UseTableColumnsOptions {
  onEdit: (todo: Todo) => void
  onDelete: (todo: Todo) => void
  /** Switch 切换完成状态 */
  onToggle: (todo: Todo) => void
  permission: PermissionConfig
}

export function useTableColumns({ onEdit, onDelete, onToggle, permission }: UseTableColumnsOptions) {
  const columns: TableColumnType<Todo>[] = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '标题', dataIndex: 'title' },
    {
      title: '状态',
      dataIndex: 'done',
      width: 140,
      customRender: ({ record }) => (
        <Switch
          checked={record.done}
          checkedChildren="完成"
          unCheckedChildren="未完"
          disabled={!permission.canEdit}
          onChange={() => onToggle(record as Todo)}
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      customRender: ({ record }) => (
        <>
          {permission.canEdit && (
            <Button type="link" size="small" onClick={() => onEdit(record as Todo)}>
              编辑
            </Button>
          )}
          {permission.canDelete && (
            <Popconfirm title="确认删除这条待办？" onConfirm={() => onDelete(record as Todo)}>
              <Button type="link" size="small" danger>
                删除
              </Button>
            </Popconfirm>
          )}
        </>
      ),
    },
  ]

  return columns
}
```

- [ ] **Step 6: 跑测试确认通过**

```bash
cd frontend && npm run test
```

Expected: 11 个测试全 PASS。

- [ ] **Step 7: 验证构建（含 tsx 编译与类型检查）**

```bash
cd frontend && npm run build
```

Expected: 构建成功。

- [ ] **Step 8: Commit**

```bash
git add frontend/src/views/list/hooks
git commit -m "feat: use-search / use-table-columns 配置化 hooks 及单测"
```

---

### Task 5: 抽屉组件与列表页组装

**Files:**
- Create: `frontend/src/views/list/components/todo-form-drawer.vue`
- Modify: `frontend/src/views/list/index.vue`（占位替换为完整实现）

**Interfaces:**
- Consumes: api 层、useSearch、useTableColumns、usePermissionConfig、Todo 类型
- Produces: 功能完整的列表页。

- [ ] **Step 1: 写 `frontend/src/views/list/components/todo-form-drawer.vue`**

```vue
<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Todo } from '../../../api/type'

const props = defineProps<{
  open: boolean
  /** 传入 = 编辑该条；null = 新增 */
  todo: Todo | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  save: [payload: { id?: number; title: string; done: boolean }]
}>()

const title = ref('')
const done = ref(false)

// 抽屉每次打开时，用传入的 todo 初始化表单
watch(
  () => props.open,
  (open) => {
    if (open) {
      title.value = props.todo?.title ?? ''
      done.value = props.todo?.done ?? false
    }
  },
)

function handleOk() {
  const text = title.value.trim()
  if (!text) return
  emit('save', { id: props.todo?.id, title: text, done: done.value })
  emit('update:open', false)
}

function handleCancel() {
  emit('update:open', false)
}
</script>

<template>
  <a-drawer :open="open" :title="todo ? '编辑待办' : '新增待办'" @close="handleCancel">
    <a-form layout="vertical">
      <a-form-item label="标题" required>
        <a-input
          v-model:value="title"
          placeholder="输入待办标题"
          @keydown.enter="handleOk"
        />
      </a-form-item>
      <a-form-item v-if="todo" label="完成状态">
        <a-switch v-model:checked="done" checked-children="完成" un-checked-children="未完" />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleOk">保存</a-button>
          <a-button @click="handleCancel">取消</a-button>
        </a-space>
      </a-form-item>
    </a-form>
  </a-drawer>
</template>
```

- [ ] **Step 2: 用完整实现替换 `frontend/src/views/list/index.vue`**

```vue
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { createTodo, deleteTodo, listTodos, updateTodo } from '../../api'
import type { Todo } from '../../api/type'
import { usePermissionConfig } from '../../use-permission-config'
import { useSearch } from './hooks/use-search'
import { useTableColumns } from './hooks/use-table-columns'
import TodoFormDrawer from './components/todo-form-drawer.vue'

// ---------- 权限 ----------
const permission = usePermissionConfig()

// ---------- 搜索（模型 + 字段配置，本地过滤） ----------
const { model: searchModel, fields: searchFields, reset: resetSearch } = useSearch()

// ---------- 数据 ----------
const todos = ref<Todo[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    todos.value = await listTodos()
  } finally {
    loading.value = false
  }
}

onMounted(load)

// 后端只有全量 GET，关键词/状态过滤都在前端做
const filtered = computed(() =>
  todos.value.filter((t) => {
    const hitKeyword = t.title.includes(searchModel.keyword.trim())
    const hitStatus =
      searchModel.status === 'all'
        ? true
        : searchModel.status === 'done'
          ? t.done
          : !t.done
    return hitKeyword && hitStatus
  }),
)

// ---------- 表格事件 ----------
async function handleToggle(todo: Todo) {
  await updateTodo({ ...todo, done: !todo.done })
  await load()
}

async function handleDelete(todo: Todo) {
  await deleteTodo(todo.id)
  await load()
}

const columns = useTableColumns({
  onEdit: (todo) => openDrawer(todo),
  onDelete: handleDelete,
  onToggle: handleToggle,
  permission,
})

// ---------- 抽屉（新增/编辑共用） ----------
const drawerOpen = ref(false)
const editing = ref<Todo | null>(null)

function openDrawer(todo: Todo | null) {
  editing.value = todo
  drawerOpen.value = true
}

async function handleSave(payload: { id?: number; title: string; done: boolean }) {
  if (payload.id === undefined) {
    await createTodo(payload.title)
  } else {
    await updateTodo({ id: payload.id, title: payload.title, done: payload.done })
  }
  await load()
}
</script>

<template>
  <a-card>
    <!-- 搜索栏：按 use-search 的 fields 配置渲染 -->
    <a-form layout="inline" style="margin-bottom: 16px">
      <a-form-item v-for="field in searchFields" :key="field.key" :label="field.label">
        <a-input
          v-if="field.component === 'input'"
          v-model:value="searchModel[field.key]"
          :placeholder="field.placeholder"
          allow-clear
          style="width: 200px"
        />
        <a-select
          v-else
          v-model:value="searchModel[field.key]"
          :options="field.options"
          style="width: 140px"
        />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="load">查询</a-button>
          <a-button @click="resetSearch">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <!-- 工具栏：新增（受权限点控制） -->
    <a-button
      v-if="permission.canAdd"
      type="primary"
      style="margin-bottom: 16px"
      @click="openDrawer(null)"
    >
      新增待办
    </a-button>

    <!-- 表格：antd 自带前端分页 -->
    <a-table
      row-key="id"
      :columns="columns"
      :data-source="filtered"
      :loading="loading"
      :pagination="{ pageSize: 5, showSizeChanger: false }"
    />

    <!-- 新增/编辑抽屉 -->
    <TodoFormDrawer v-model:open="drawerOpen" :todo="editing" @save="handleSave" />
  </a-card>
</template>
```

- [ ] **Step 3: 全量验证（单测 + 类型检查 + 构建）**

```bash
cd frontend && npm run test && npm run build
```

Expected: 11 个测试全 PASS；vue-tsc 无类型错误；构建成功。

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views src/main/resources/static
git commit -m "feat: 列表页组装（搜索+表格+分页+抽屉）与表单抽屉组件"
```

---

### Task 6: 集成验证与 README 更新

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: Task 1-5 全部产出、后端 8081。
- Produces: 验收达标的项目与文档。

- [ ] **Step 1: 构建最新产物**

```bash
cd frontend && npm run build
```

- [ ] **Step 2: 启动后端（run_in_background）**

```bash
./mvnw spring-boot:run
```

等待：轮询 `curl -s http://localhost:8081/api/todos` 直到返回 JSON（首次跑要下载依赖，超时 3 分钟）。

- [ ] **Step 3: 验证 8081 托管页面与 H2 控制台**

```bash
curl -s http://localhost:8081/
curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/h2-console
```

Expected: 第 1 条 HTML 含 `<div id="app">` 与 `assets/index-*.js`；第 2 条返回 `200`。

- [ ] **Step 4: API 冒烟（确认链路）**

```bash
curl -s -X POST http://localhost:8081/api/todos -H "Content-Type: application/json" -d '{"title":"集成验证"}'
```

记下返回的 id（设为 N），然后：

```bash
curl -s -X PUT http://localhost:8081/api/todos/N -H "Content-Type: application/json" -d '{"title":"集成验证","done":true}'
curl -s -X DELETE http://localhost:8081/api/todos/N
curl -s http://localhost:8081/api/todos
```

Expected: PUT 返回 done=true；DELETE 无输出；GET 列表无「集成验证」。

- [ ] **Step 5: 验证 Vite 开发模式（run_in_background 起 dev）**

```bash
cd frontend && npm run dev
```

然后 `curl -s http://localhost:5173/`：Expected 含 `<div id="app">` 且引用 `/src/main.ts`。验证后停掉 dev 与后端进程。

- [ ] **Step 6: 更新 README.md**

1. 标题改为 `# 前后端分离示例（Spring Boot + Vue 3 + TypeScript）`；简介改为：前端用 Vue 3 + TypeScript + ant-design-vue，按中后台模块结构组织（api/constant/权限/hooks/组装层），后端四层结构不变。
2. 环境要求追加：`Node.js 20 及以上（构建前端用）`。
3. 技术栈表前端行改为 `| 前端 | Vue 3 + TypeScript + ant-design-vue（Vite） | 组合式 API、按需引入、hash 路由 |`。
4. 目录结构 `frontend/` 子树替换为：

```
└── frontend/                       # 前端代码（Vue 3 + TS + antd，与后端分离）
    ├── package.json / vite.config.ts / tsconfig.json
    ├── index.html                      # Vite 入口页
    └── src/
        ├── main.ts                     # 入口：挂载 router
        ├── App.vue                     # 外壳：布局 + 侧边导航 + RouterView
        ├── router/index.ts             # hash 路由：/list、/operation-log
        ├── api/
        │   ├── index.ts                # 接口怎么调（函数）
        │   └── type.ts                 # 数据长什么样（类型）
        ├── constant/index.ts           # 状态筛选等固定值
        ├── use-permission-config.ts    # 按钮权限（静态演示）
        └── views/
            ├── list/                   # 页面主体
            │   ├── index.vue           # 组装层：搜索+表格+分页+抽屉
            │   ├── hooks/
            │   │   ├── use-search.tsx        # 搜索栏配置
            │   │   └── use-table-columns.tsx # 表格列配置（JSX）
            │   └── components/
            │       └── todo-form-drawer.vue  # 新增/编辑表单抽屉
            └── operation-log/index.vue  # 操作日志空壳页
```

   并在 resources 子树 `data.sql` 行后补：`│       └── static/（npm run build 产物，后端直接托管）`（树形符号与现有保持一致）。
5. 「三、怎么运行」第 2 步整段替换为：

```
### 第 2 步：启动前端

两种方式任选：

**开发模式（推荐，改代码保存即热更新）** —— 新开一个终端：

```bash
cd frontend
npm install        # 第一次需要
npm run dev
```

浏览器打开： **http://localhost:5173**
（`/api` 请求由 Vite 自动代理给 8081 后端，无需关心跨域。）

**正式模式（一个端口跑全栈）**：

```bash
cd frontend
npm run build      # 产物输出到 src/main/resources/static
```

然后（重新）启动后端，浏览器打开： **http://localhost:8081**（自动进入 `#/list`）。
```

6. FAQ 第 3 问前端答句改为「开发模式下保存即热更新；正式模式改完要重新 `npm run build`（并重启后端）」。
7. 「下一步可以怎么改着玩」：把「把前端换成 Vue 或 React」替换为「把 `use-permission-config.ts` 接上真实登录态，体验按钮级权限」「给 operation-log 页补上真实日志数据」两条。

- [ ] **Step 7: Commit**

```bash
git add README.md
git commit -m "docs: README 更新为 Vue3+TS+antd 中后台结构说明"
```

- [ ] **Step 8: 请用户浏览器验收**

- http://localhost:8081：`#/list` 的增删改查（抽屉新增/编辑、Switch 切换、Popconfirm 删除、搜索、分页）、侧边导航切 `#/operation-log`。
- `cd frontend && npm run dev` 后 http://localhost:5173：同样操作 + 热更新。

---

## Self-Review 记录

- **Spec coverage**：目录结构全要素（Task 1 骨架 / Task 2 api 双文件 / Task 3 constant+权限+路由+空壳页 / Task 4 两个 hooks / Task 5 抽屉+组装层）；按需引入+JSX+hash 路由+前端过滤分页（Task 1/3/4/5）；11 个单测（2/3/4）；验收 1-5（各 Task + Task 6）；README（Task 6）。后端零改动为全局约束。
- **Placeholder scan**：无 TBD/空引用；每步均有完整代码或命令。
- **Type consistency**：`Todo`/api 四函数签名在 Task 2 定义、Task 5 消费一致；`useSearch` 返回结构 Task 4 定义与 Task 5 解构一致；`useTableColumns` 的 options（onEdit/onDelete/onToggle/permission）两任务一致；`SearchModel.status` 用 `string`（与 STATUS_OPTIONS 的 value、模板绑定兼容）。
