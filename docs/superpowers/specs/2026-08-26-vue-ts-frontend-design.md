# 前端改造：原生 JS → Vue 3 + TypeScript（中后台模块结构）设计文档

日期：2026-08-26（同日修订：改为中后台管理页结构 + ant-design-vue + vue-router）
状态：已确认（用户于对话中批准修订版）

## 1. 背景与目标

现有项目是 Spring Boot（端口 8081）+ 原生 JavaScript 的前后端分离 Todo 示例。
目标：把前端技术栈更换为 **Vue 3 + TypeScript + ant-design-vue**，并按**中后台管理页模块结构**组织代码
（api 函数/类型分离、constant、权限配置、views 组装层、hooks 配置化搜索/表格列、抽屉子组件、多页路由），
后端代码不做任何改动。定位是练手：用小项目学会这套真实中后台的代码组织方式。

## 2. 后端接口（不变，仅作契约参考）

统一前缀 `/api/todos`：

| 方法 | 路径 | 请求体 | 返回 |
|------|------|--------|------|
| GET | /api/todos | - | `Todo[]` |
| POST | /api/todos | `{"title":"xxx"}` | `Todo` |
| PUT | /api/todos/{id} | `{"title":"xxx","done":true}` | `Todo` |
| DELETE | /api/todos/{id} | - | void |

`Todo` 字段：`id: number`、`title: string`、`done: boolean`。
后端只有全量 GET，**搜索与分页均在前端本地做**（关键词 + 状态过滤，antd Table 自带分页）。
CORS 配置保持不动（开发走 Vite 代理时同源，留作直连兜底）。

## 3. 目录结构

`frontend/` 整体替换为 Vite + Vue 3 + TS 项目，`src/` 即示例中的模块根：

```
frontend/src/
├── main.ts                   # 入口：挂载 router
├── App.vue                   # 外壳：布局 + 侧边导航 + <RouterView>
├── router/index.ts           # hash 路由：/list、/operation-log
├── api/
│   ├── index.ts              # 接口怎么调：listTodos/createTodo/updateTodo/deleteTodo
│   └── type.ts               # 数据长什么样：Todo 等类型
├── constant/index.ts         # 下拉选项等固定值：状态筛选项
├── use-permission-config.ts  # 按钮权限：canAdd/canEdit/canDelete（静态演示）
└── views/
    ├── list/                 # 页面主体
    │   ├── index.vue         # 组装层：搜索栏 + 表格 + 分页 + 抽屉
    │   ├── hooks/
    │   │   ├── use-search.tsx        # 搜索栏配置（模型 + 字段定义 + reset）
    │   │   └── use-table-columns.tsx # 表格列配置（.tsx：状态/操作列 JSX 渲染）
    │   └── components/
    │       └── todo-form-drawer.vue  # 新增/编辑表单抽屉
    └── operation-log/
        └── index.vue         # 操作日志空壳占位页
```

旧的 `index.html`、`app.js`、`style.css` 删除（git 历史保留）。界面为 antd 管理页风格，**不再保持**原卡片样式。

## 4. 关键设计决策

- **组合式 API**：统一 `<script setup lang="ts">`。
- **UI**：ant-design-vue 4，`unplugin-vue-components` + `AntDesignVueResolver` **按需自动引入**（SFC 模板直接写 `<a-xxx>`；`.tsx` 里 JSX 不走模板编译，Switch/Button/Popconfirm 需显式 import）。
- **JSX**：`@vitejs/plugin-vue-jsx` 编译 `.tsx` hooks；列的 customRender、后续搜索项自定义渲染都写 JSX。
- **路由**：vue-router 4，**hash 模式**（`createWebHashHistory`）——构建产物由 Spring Boot 静态托管，深链刷新不需要后端加 SPA 转发，符合"后端零改动"。
- **状态**：列表页局部 `ref`/`computed`，不引入 Pinia（YAGNI）。
- **hooks 模式**：`useSearch()` 返回 `{ model, fields, reset }`，index.vue 按 `fields` 配置渲染搜索表单；`useTableColumns({ onEdit, onDelete, onToggle, permission })` 返回 antd 列数组，状态列渲染 Switch（点击即 PUT 切换）、操作列渲染编辑/删除（Popconfirm）。
- **抽屉**：新增/编辑共用一个 `todo-form-drawer.vue`，`todo === null` 表示新增。
- **权限**：`usePermissionConfig()` 静态返回全开，演示"视图层按权限点控制按钮显隐"的写法；无登录体系。
- **api 层**：类型（`api/type.ts`）与函数（`api/index.ts`）分文件；fetch 封装同前，相对路径 `/api/todos`。

## 5. 开发与构建

`vite.config.ts`：

- plugins：vue + vueJsx + Components(AntDesignVueResolver)
- `server.proxy`：`'/api' → http://localhost:8081`（开发 `npm run dev` 于 5173，热更新）
- `build.outDir`：`'../src/main/resources/static'`，显式 `emptyOutDir: true`，该目录保持 git 跟踪（clone 后不用 npm 也能跑）
- `test.environment: 'jsdom'`

使用方式：开发 = 后端 8081 + `npm run dev` 访问 http://localhost:5173；正式 = `npm run build` 后（重）启 Spring Boot 访问 http://localhost:8081（自动跳 `#/list`）。

## 6. 依赖版本

vue ^3.5、ant-design-vue ^4.2、vue-router ^4.5、typescript ~5.8、vite ^7、@vitejs/plugin-vue ^6、
@vitejs/plugin-vue-jsx ^4、unplugin-vue-components（^28，装不上取 latest）、vitest ^3、jsdom。
以 npm 实际可解析的当前稳定版为准。**不引入** Pinia、axios、UI 二次封装库。

## 7. 测试与验收

单测（vitest，不挂载 antd 组件、只测纯逻辑/配置结构）：

- `api/index.ts`：mock 全局 fetch，验证四个方法的 URL、方法、请求体及返回解析（4 个）。
- `use-search.tsx`：初始模型、字段配置、reset（3 个）。
- `use-table-columns.tsx`：列结构（ID/标题/状态/操作）、状态与操作列有 customRender、权限关闭时列结构不变（3 个）。
- `use-permission-config.ts`：返回三个权限点（1 个）。

验收标准（全部满足才算完成）：

1. `npm run build`（含 vue-tsc 类型检查）通过，产物输出到 `src/main/resources/static`。
2. `npm run test` 11 个单测全绿。
3. 后端启动后访问 http://localhost:8081：自动进入 `#/list`；新增（抽屉）、编辑（抽屉改标题/状态）、Switch 切换完成、Popconfirm 删除、关键词/状态搜索过滤、分页均可用；侧边导航可切到 `#/operation-log` 空壳页。
4. 开发模式 http://localhost:5173 同样可用且有热更新。
5. 旧静态文件已删除；`.gitignore` 覆盖 `node_modules/`；README 更新为新结构、新运行说明。

## 8. 范围外

- 后端任何改动（含 CORS、端口、接口、SPA 转发）。
- 真实登录/权限体系（权限仅静态演示）、真实操作日志数据。
- 新的待办业务功能（批量操作、导出等）。
- CI/CD、ESLint/Prettier 等工程化配置。
