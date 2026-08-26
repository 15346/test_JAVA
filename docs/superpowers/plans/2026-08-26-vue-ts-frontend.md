# 前端改造 Vue 3 + TypeScript 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `frontend/` 从原生 JS 静态页替换为 Vite + Vue 3 + TypeScript 单页应用，功能与界面不变，构建产物由 Spring Boot 托管。

**Architecture:** 前端独立 `frontend/` 目录（npm 项目），组合式 API；`api/todo.ts` 类型化封装后端 REST；`App.vue` 持有状态，`TodoItem.vue` 展示单条。开发走 5173 + Vite 代理，构建输出到 `src/main/resources/static` 由 8081 托管。

**Tech Stack:** Vue ^3.5、TypeScript ~5.8、Vite ^7、vitest + @vue/test-utils（单测）。后端 Spring Boot 不动。

**Spec:** `docs/superpowers/specs/2026-08-26-vue-ts-frontend-design.md`

## Global Constraints

- 后端代码**零改动**（含 CorsConfig、application.properties、接口）。
- 后端端口 **8081**，Vite 开发端口 **5173**，`/api` 代理到 `http://localhost:8081`。
- 接口契约：`GET/POST /api/todos`、`PUT/DELETE /api/todos/{id}`，`Todo { id: number; title: string; done: boolean }`。
- 统一 `<script setup lang="ts">`；**不引入** axios、Pinia、Vue Router、UI 组件库。
- `npm run build` 产物输出到 `src/main/resources/static`，该目录**保持 git 跟踪**（clone 后不用 npm 也能跑）。
- 界面与原版一致（样式原样迁移，仅选择器适配）。
- 所有 shell 命令在项目根 `c:/Users/爱速智/Projects/test` 下执行；前端命令需先 `cd frontend`（每次 Bash 调用独立，不能依赖上一次的 cd）。

---

### Task 1: Vite + Vue 3 + TS 项目骨架

**Files:**
- Delete: `frontend/index.html`、`frontend/app.js`、`frontend/style.css`
- Create: `frontend/package.json`、`frontend/vite.config.ts`、`frontend/tsconfig.json`、`frontend/index.html`、`frontend/src/main.ts`、`frontend/src/App.vue`（占位）
- Modify: `.gitignore`（追加 node_modules/）

**Interfaces:**
- Consumes: 无（首个任务）
- Produces: 可 `npm run dev` / `npm run build` 的空壳应用；`npm run test`（vitest，暂无测试文件，退出码 0）；`test` 脚本供 Task 2/3 使用。

- [ ] **Step 1: 删除旧前端三件**

```bash
git rm frontend/index.html frontend/app.js frontend/style.css
```

- [ ] **Step 2: 创建 `frontend/package.json`**

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
    "vue": "^3.5.13"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^6.0.0",
    "@vue/test-utils": "^2.4.6",
    "jsdom": "^26.1.0",
    "typescript": "~5.8.3",
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

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  // 开发时：npm run dev 起在 5173，/api 开头的请求代理给后端 8081
  server: {
    proxy: {
      '/api': 'http://localhost:8081',
    },
  },
  // 构建：产物直接输出到 Spring Boot 的静态资源目录，后端 8081 托管
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
  // 单测：组件测试需要 DOM 环境
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
    "strict": true,
    "noEmit": true,
    "isolatedModules": true,
    "skipLibCheck": true,
    "types": ["vite/client"]
  },
  "include": ["src/**/*.ts", "src/**/*.d.ts", "src/**/*.vue", "vite.config.ts"]
}
```

- [ ] **Step 5: 创建 `frontend/index.html`**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>待办清单 · 前后端分离示例</title>
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
createApp(App).mount('#app')
```

- [ ] **Step 7: 创建占位 `frontend/src/App.vue`**

```vue
<script setup lang="ts">
</script>

<template>
  <div class="container">
    <h1>📝 待办清单</h1>
    <p class="subtitle">Spring Boot 后端 + Vue 3 + TypeScript 前端（搭建中）</p>
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

Expected: 依赖安装成功；`vue-tsc --noEmit` 无报错；vite 输出 `✓ built in ...`；`ls ../src/main/resources/static` 可见 `index.html` 与 `assets/` 目录。

- [ ] **Step 10: 验证测试脚本空跑**

```bash
cd frontend && npm run test
```

Expected: `No test files found` 但退出码为 0（vitest run 无文件时 exit 0；若版本行为不同报非零，记录实际输出，不阻塞）。

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "feat: 前端替换为 Vite + Vue 3 + TypeScript 项目骨架"
```

---

### Task 2: api/todo.ts 类型化接口层（TDD）

**Files:**
- Create: `frontend/src/api/todo.ts`
- Test: `frontend/src/api/todo.test.ts`

**Interfaces:**
- Consumes: 无（纯 fetch 封装）
- Produces（Task 3/4 依赖，签名必须一致）:
  - `export interface Todo { id: number; title: string; done: boolean }`
  - `listTodos(): Promise<Todo[]>`
  - `createTodo(title: string): Promise<Todo>`
  - `updateTodo(todo: Todo): Promise<Todo>`
  - `deleteTodo(id: number): Promise<void>`

- [ ] **Step 1: 写失败测试 `frontend/src/api/todo.test.ts`**

```ts
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createTodo, deleteTodo, listTodos, updateTodo } from './todo'

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

Expected: FAIL，报 `Failed to resolve import "./todo"`（模块还不存在）。

- [ ] **Step 3: 写实现 `frontend/src/api/todo.ts`**

```ts
// ============================================================
//  接口层：用 fetch 调用后端 REST 接口，TypeScript 保证类型安全。
//  开发时走相对路径 /api/todos，由 Vite 代理到 http://localhost:8081；
//  构建后由 Spring Boot 在同源 8081 下直接提供服务。
// ============================================================

const API = '/api/todos'

/** 一条待办 —— 对应后端 com.example.demo.entity.Todo */
export interface Todo {
  id: number
  title: string
  done: boolean
}

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

- [ ] **Step 4: 跑测试确认通过**

```bash
cd frontend && npm run test
```

Expected: 4 个测试全 PASS。

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api
git commit -m "feat: 前端接口层 todo.ts（Todo 类型 + fetch 封装）及单测"
```

---

### Task 3: TodoItem.vue 单条待办组件（TDD）

**Files:**
- Create: `frontend/src/components/TodoItem.vue`
- Test: `frontend/src/components/TodoItem.test.ts`

**Interfaces:**
- Consumes: `import type { Todo } from '../api/todo'`（Task 2 产出）
- Produces（Task 4 依赖）: 组件 props `{ todo: Todo }`；emits `toggle`（无参）、`remove`（无参）；根元素 `<li>`，勾选框 `input[type="checkbox"]`、标题 `.title`、删除按钮 `.del`、完成态加类 `done`。

- [ ] **Step 1: 写失败测试 `frontend/src/components/TodoItem.test.ts`**

```ts
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import TodoItem from './TodoItem.vue'

const todo = { id: 7, title: '写周报', done: false }

describe('TodoItem', () => {
  it('渲染标题，复选框未勾选', () => {
    const wrapper = mount(TodoItem, { props: { todo } })
    expect(wrapper.find('.title').text()).toBe('写周报')
    const box = wrapper.find('input[type="checkbox"]').element as HTMLInputElement
    expect(box.checked).toBe(false)
  })

  it('done=true 时 li 加 done 类且复选框勾选', () => {
    const wrapper = mount(TodoItem, { props: { todo: { ...todo, done: true } } })
    expect(wrapper.find('li').classes()).toContain('done')
    const box = wrapper.find('input[type="checkbox"]').element as HTMLInputElement
    expect(box.checked).toBe(true)
  })

  it('点击复选框 emit toggle', async () => {
    const wrapper = mount(TodoItem, { props: { todo } })
    await wrapper.find('input[type="checkbox"]').setValue(true)
    expect(wrapper.emitted('toggle')).toHaveLength(1)
  })

  it('点击删除按钮 emit remove', async () => {
    const wrapper = mount(TodoItem, { props: { todo } })
    await wrapper.find('.del').trigger('click')
    expect(wrapper.emitted('remove')).toHaveLength(1)
  })
})
```

- [ ] **Step 2: 跑测试确认失败**

```bash
cd frontend && npm run test
```

Expected: 新增 4 个用例 FAIL（无法解析 `./TodoItem.vue`），Task 2 的 4 个仍 PASS。

- [ ] **Step 3: 写实现 `frontend/src/components/TodoItem.vue`**

```vue
<script setup lang="ts">
import type { Todo } from '../api/todo'

// 单条待办：只负责展示和上报事件，接口调用都集中在 App.vue
defineProps<{ todo: Todo }>()
defineEmits<{ toggle: []; remove: [] }>()
</script>

<template>
  <li :class="{ done: todo.done }">
    <input type="checkbox" :checked="todo.done" @change="$emit('toggle')" />
    <span class="title">{{ todo.title }}</span>
    <button class="del" @click="$emit('remove')">删除</button>
  </li>
</template>
```

- [ ] **Step 4: 跑测试确认通过**

```bash
cd frontend && npm run test
```

Expected: 8 个测试全 PASS。

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components
git commit -m "feat: TodoItem 单条待办组件及单测"
```

---

### Task 4: App.vue 组装与样式迁移

**Files:**
- Modify: `frontend/src/App.vue`（占位替换为完整版）

**Interfaces:**
- Consumes: `listTodos/createTodo/updateTodo/deleteTodo/Todo`（Task 2）、`TodoItem`（Task 3）
- Produces: 功能完整的最终界面。

- [ ] **Step 1: 用下面内容整体替换 `frontend/src/App.vue`**

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { createTodo, deleteTodo, listTodos, updateTodo, type Todo } from './api/todo'
import TodoItem from './components/TodoItem.vue'

// 全部待办。ref() 包一层，改 .value 才会触发界面更新
const todos = ref<Todo[]>([])
// 输入框内容，v-model 双向绑定
const title = ref('')

// 页面打开时先加载一次 (GET)
onMounted(load)

async function load() {
  todos.value = await listTodos()
}

// 新增 (POST)：回车或点「添加」
async function add() {
  const text = title.value.trim()
  if (!text) return
  await createTodo(text)
  title.value = ''
  await load()
}

// 切换完成状态 (PUT)
async function toggle(item: Todo) {
  await updateTodo({ ...item, done: !item.done })
  await load()
}

// 删除 (DELETE)
async function remove(item: Todo) {
  await deleteTodo(item.id)
  await load()
}
</script>

<template>
  <div class="container">
    <h1>📝 待办清单</h1>
    <p class="subtitle">Spring Boot 后端 + Vue 3 + TypeScript 前端</p>

    <!-- 新增一条 -->
    <div class="add-row">
      <input
        v-model="title"
        type="text"
        placeholder="输入待办事项，回车或点添加..."
        autofocus
        @keydown.enter="add"
      />
      <button @click="add">添加</button>
    </div>

    <!-- 待办列表 -->
    <ul>
      <TodoItem
        v-for="todo in todos"
        :key="todo.id"
        :todo="todo"
        @toggle="toggle(todo)"
        @remove="remove(todo)"
      />
    </ul>

    <p class="tip">
      后端接口地址：<code>/api/todos</code>（开发时由 Vite 代理到 http://localhost:8081）<br />
      打开 <a href="http://localhost:8081/h2-console" target="_blank">H2 数据库控制台</a> 可直接看表数据。
    </p>
  </div>
</template>

<!-- 全局样式：由原 frontend/style.css 迁移而来（含 body 级规则，故不加 scoped）。 -->
<!-- 原页面用 id 选择器 #title/#addBtn，这里没有 id，改为 .add-row 内的元素选择器，视觉不变。 -->
<style>
* {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
}

body {
    font-family: -apple-system, "Segoe UI", "Microsoft YaHei", sans-serif;
    background: #f4f6f8;
    color: #333;
    line-height: 1.6;
    padding: 40px 16px;
}

.container {
    max-width: 520px;
    margin: 0 auto;
    background: #fff;
    border-radius: 12px;
    padding: 28px 24px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

h1 {
    font-size: 26px;
    text-align: center;
}

.subtitle {
    text-align: center;
    color: #888;
    font-size: 13px;
    margin-bottom: 20px;
}

.add-row {
    display: flex;
    gap: 8px;
    margin-bottom: 18px;
}

.add-row input {
    flex: 1;
    padding: 10px 12px;
    border: 1px solid #ddd;
    border-radius: 8px;
    font-size: 15px;
    outline: none;
}

.add-row input:focus {
    border-color: #4a90d9;
}

.add-row button {
    padding: 10px 18px;
    background: #4a90d9;
    color: #fff;
    border: none;
    border-radius: 8px;
    font-size: 15px;
    cursor: pointer;
}

.add-row button:hover {
    background: #3a7bc8;
}

ul {
    list-style: none;
}

li {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 4px;
    border-bottom: 1px solid #f0f0f0;
}

li .title {
    flex: 1;
}

li.done .title {
    text-decoration: line-through;
    color: #aaa;
}

li input[type="checkbox"] {
    width: 18px;
    height: 18px;
    cursor: pointer;
}

.del {
    background: none;
    border: none;
    color: #e0604e;
    cursor: pointer;
    font-size: 14px;
}

.del:hover {
    text-decoration: underline;
}

.tip {
    margin-top: 22px;
    font-size: 12px;
    color: #999;
    text-align: center;
    line-height: 1.8;
}

.tip a {
    color: #4a90d9;
}
</style>
```

- [ ] **Step 2: 全量验证（类型检查 + 构建 + 单测）**

```bash
cd frontend && npm run build && npm run test
```

Expected: vue-tsc 无类型错误；vite 构建成功；8 个测试全 PASS。

- [ ] **Step 3: Commit**

```bash
git add frontend/src/App.vue src/main/resources/static
git commit -m "feat: App.vue 组装完整待办功能并迁移原样式"
```

---

### Task 5: 集成验证与 README 更新

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: Task 1-4 全部产出、后端 8081 服务。
- Produces: 验收达标的完整项目与更新后的文档。

- [ ] **Step 1: 构建最新产物**

```bash
cd frontend && npm run build
```

Expected: 构建成功，`../src/main/resources/static` 已更新。

- [ ] **Step 2: 启动后端（后台）**

```bash
./mvnw spring-boot:run
```

（run_in_background 执行）Expected: 日志出现 `Started DemoApplication`；首次运行会下载依赖，稍等。等待方式：轮询 `curl -s http://localhost:8081/api/todos` 直到返回 JSON（超时 3 分钟）。

- [ ] **Step 3: 验证 Spring Boot 托管的前端页面**

```bash
curl -s http://localhost:8081/
```

Expected: HTML 含 `<div id="app">` 与 `assets/index-*.js` 引用。

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/h2-console
```

Expected: `200`。

- [ ] **Step 4: API 增删改查冒烟（后端未动，确认链路完好）**

```bash
curl -s -X POST http://localhost:8081/api/todos -H "Content-Type: application/json" -d '{"title":"集成验证"}'
```

Expected: 返回 `{"id":N,"title":"集成验证","done":false}`。记下 N，继续：

```bash
curl -s -X PUT http://localhost:8081/api/todos/N -H "Content-Type: application/json" -d '{"title":"集成验证","done":true}'
curl -s -X DELETE http://localhost:8081/api/todos/N
curl -s http://localhost:8081/api/todos
```

Expected: PUT 返回 done=true 的对象；DELETE 无输出；GET 列表中不再有「集成验证」。

- [ ] **Step 5: 验证 Vite 开发模式**

```bash
cd frontend && npm run dev
```

（run_in_background 执行，稍候）然后：

```bash
curl -s http://localhost:5173/
```

Expected: HTML 含 `<div id="app">` 且引用 `/src/main.ts`。验证后停掉 dev 进程和后端进程。

- [ ] **Step 6: 更新 README.md**

1. 标题与简介：`# 极简前后端分离示例（Spring Boot + 原生 JS）` → `# 前后端分离示例（Spring Boot + Vue 3 + TypeScript）`；第一段改为「前端用 Vue 3 + TypeScript（Vite 构建）调用接口」，删除「故意写得特别简单：没有 React/Vue…」的引言块，改为「前端用组合式 API + TypeScript，类型安全的接口层；后端四层结构不变」。
2. 环境要求行后追加：`Node.js 20 及以上（构建前端用）`。
3. 技术栈表前端行改为：`| 前端 | Vue 3 + TypeScript（Vite） | 组合式 API、类型安全 |`。
4. 目录结构中 `frontend/` 子树替换为：

```
└── frontend/                       # 前端代码（Vue 3 + TypeScript，与后端分离）
    ├── package.json / vite.config.ts / tsconfig.json
    ├── index.html                      # Vite 入口页
    └── src/
        ├── main.ts                     # 挂载 Vue 应用
        ├── App.vue                     # 根组件：输入框 + 列表 + 状态
        ├── api/todo.ts                 # 接口层：Todo 类型 + fetch 封装
        └── components/TodoItem.vue     # 单条待办组件
```

   并在 `resources/` 子树中 `data.sql` 行后补一行：`│       └── static/（npm run build 产物，后端直接托管）`（注意与现有 `application.properties`、`data.sql` 的树形符号保持一致）。
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
（`/api` 请求会由 Vite 自动代理给 8081 的后端，无需关心跨域。）

**正式模式（一个端口跑全栈）** —— 构建后由后端托管：

```bash
cd frontend
npm run build      # 产物输出到 src/main/resources/static
```

然后（重新）启动后端，浏览器打开： **http://localhost:8081**
```

6. FAQ 第 3 问「改了代码怎么生效？」的答句改为：后端同原文；前端改为「开发模式下保存即热更新；正式模式改完要重新 `npm run build`（并重启后端）」。
7. 「七、下一步可以怎么改着玩」中「把前端换成 Vue 或 React」一条替换为「给 `TodoItem.vue` 加双击标题编辑」。

- [ ] **Step 7: Commit**

```bash
git add README.md
git commit -m "docs: README 更新为 Vue 3 + TypeScript 前端说明"
```

- [ ] **Step 8: 请用户做最终验收**

提示用户亲自在浏览器验证两处（自动化只能覆盖到 HTML/API 层）：
- http://localhost:8081（构建版）：增、删、勾选、界面与原版一致。
- `cd frontend && npm run dev` 后访问 http://localhost:5173：同样操作 + 修改代码保存后热更新生效。

---

## Self-Review 记录

- **Spec coverage**：接口契约/类型（Task 2）、组件（Task 3）、状态与数据流（Task 4）、代理与 outDir（Task 1）、旧文件删除与 .gitignore（Task 1）、单测两文件（Task 2/3）、验收标准 1-5（Task 1/4/5 + 用户验收）、README（Task 5）——均有对应任务。后端零改动为全局约束。
- **Placeholder scan**：无 TBD/「适当处理」类占位；所有代码步骤给出完整代码。
- **Type consistency**：`Todo` 字段、`listTodos/createTodo(title)/updateTodo(todo)/deleteTodo(id)` 签名在 Task 2 定义、Task 4 消费处一致；`TodoItem` 的 props/emits 在 Task 3 定义、Task 4 模板使用处一致。
