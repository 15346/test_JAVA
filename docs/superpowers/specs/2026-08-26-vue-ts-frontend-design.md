# 前端改造：原生 JS → Vue 3 + TypeScript 设计文档

日期：2026-08-26
状态：已确认（用户于对话中批准）

## 1. 背景与目标

现有项目是 Spring Boot（端口 8081）+ 原生 JavaScript 的前后端分离 Todo 示例：
前端为 `frontend/index.html`、`frontend/app.js`、`frontend/style.css` 三个静态文件，
通过 fetch 调用后端 REST 接口。

目标：把前端技术栈更换为 **Vue 3 + TypeScript**（Vite 构建），功能与界面保持不变，
后端代码不做任何改动。

## 2. 后端接口（不变，仅作契约参考）

统一前缀 `/api/todos`：

| 方法 | 路径 | 请求体 | 返回 |
|------|------|--------|------|
| GET | /api/todos | - | `Todo[]` |
| POST | /api/todos | `{"title":"xxx"}` | `Todo` |
| PUT | /api/todos/{id} | `{"title":"xxx","done":true}` | `Todo` |
| DELETE | /api/todos/{id} | - | void |

`Todo` 字段：`id: number`、`title: string`、`done: boolean`（对应后端实体 `com.example.demo.entity.Todo`）。

CORS 已由 `CorsConfig` 放行 `http://localhost:*`，保持不动（开发走 Vite 代理时实际同源，该配置留作直连兜底）。

## 3. 目录结构

`frontend/` 整体替换为 Vite + Vue 3 + TS 项目：

```
frontend/
  package.json
  vite.config.ts
  tsconfig.json / tsconfig.app.json / tsconfig.node.json（脚手架标准三件）
  index.html
  src/
    main.ts                 # 挂载入口
    App.vue                 # 输入框 + 列表 + todos 状态
    api/todo.ts             # Todo 接口定义 + 类型化 fetch 封装
    components/TodoItem.vue # 单条待办：复选框 / 标题 / 删除按钮
    assets/…（如脚手架生成，可清理）
```

旧的 `index.html`、`app.js`、`style.css` 删除（git 历史保留），样式迁入组件 scoped style，视觉效果与原版一致。

## 4. 关键设计决策

- **组合式 API**：统一使用 `<script setup lang="ts">`。
- **状态**：`App.vue` 持有 `const todos = ref<Todo[]>([])`，不引入 Pinia（YAGNI）。
- **组件通信**：`TodoItem.vue` props 接收 `todo: Todo`，emits `toggle` / `remove`；api 调用集中在 `App.vue`。
- **api 层**（`api/todo.ts`）：
  - `export interface Todo { id: number; title: string; done: boolean }`
  - `listTodos() / createTodo(title) / updateTodo(todo) / deleteTodo(id)`，内部用 fetch，统一前缀 `/api/todos`（相对路径，开发时由 Vite 代理）。
- **不引入** axios、Vue Router、Pinia、UI 组件库。

## 5. 开发与构建

`vite.config.ts`：

- `server.proxy`：`'/api' → http://localhost:8081`（开发时 `npm run dev` 于 5173，热更新，请求同源）。
- `build.outDir`：`'../src/main/resources/static'`，并显式 `emptyOutDir: true`（outDir 在 Vite root 之外时 Vite 不会默认清空，需显式声明）。

使用方式：

- 开发：后端跑 8081，前端 `npm run dev` 访问 http://localhost:5173。
- 正式：`npm run build` 后（重新）启动 Spring Boot，直接访问 http://localhost:8081。

## 6. 依赖版本

- vue ^3.5、typescript ~5.x、vite ^7（create-vue 脚手架生成，非本设计锁定项，以脚手架当前稳定版为准）
- 开发依赖：vitest、@vue/test-utils

## 7. 测试与验收

单测（vitest）：

- `api/todo.ts`：mock 全局 fetch，验证四个方法的 URL、方法、请求体及返回解析。
- `TodoItem.vue`：给定 todo 渲染标题与勾选状态；点击复选框 emit `toggle`；点击删除 emit `remove`。

验收标准（全部满足才算完成）：

1. `npm run build` 与 TS 类型检查（`vue-tsc`）通过，产物输出到 `src/main/resources/static`。
2. `npm run test` 单测全绿。
3. 后端启动后访问 http://localhost:8081，增、删、勾选完成全部可用，界面与原版一致（含 H2 控制台链接）。
4. 开发模式 http://localhost:5173 下同样可用且有热更新。
5. 旧静态文件已删除；`.gitignore` 覆盖 `node_modules/`、`dist`；README 的运行说明已更新为 Vue 版。

## 8. 范围外

- 后端任何改动（含 CORS、端口、接口）。
- 新功能（筛选、编辑、拖拽等均不做）。
- CI/CD、ESLint/Prettier 等工程化配置（脚手架自带选项除外）。
