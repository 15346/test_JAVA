# 前后端分离示例（Spring Boot + Vue 3 + TypeScript）

一个**待办清单（Todo）**小项目，用来学习「前后端分离」：后端用 Spring Boot 提供 REST 接口（经典
Controller → Service → Repository 四层），前端用 **Vue 3 + TypeScript + ant-design-vue**，
按**中后台管理页的模块化结构**组织（页面自治：每页自带 api / constant / hooks / components），
覆盖 **增、删、改、查（CRUD）**、搜索过滤、分页、表单抽屉、按钮权限（静态演示）。

---

## 一、技术栈

| 层 | 技术 | 作用 |
|---|---|---|
| 后端框架 | Spring Boot 4 | 快速搭建 Web 服务 |
| 数据库 | MySQL（H2 预留可切回） | 持久化存储 |
| 数据访问 | Spring Data JPA | 几乎不用写 SQL 就能增删改查 |
| 前端 | Vue 3 + TypeScript + ant-design-vue（Vite） | 组合式 API、按需引入、hash 路由 |

环境要求：**JDK 17 及以上**（本项目用 21）、**Node.js 20 及以上**（构建前端用）。

---

## 二、目录结构

```
test/
├── pom.xml                         # Maven 依赖、构建配置
├── mvnw / mvnw.cmd                 # Maven 包装器（用它就不用单独装 Maven）
├── src/main/
│   ├── java/com/example/demo/
│   │   ├── DemoApplication.java        # ① 启动入口（main 方法在这里）
│   │   ├── entity/Todo.java            # ② 实体：对应数据库 todo 表
│   │   ├── repository/TodoRepository.java  # ③ 数据访问层：和数据库打交道
│   │   ├── service/TodoService.java    # ④ 业务逻辑层
│   │   ├── controller/TodoController.java  # ⑤ 接口层：定义前端能调的 API
│   │   └── config/CorsConfig.java      # 跨域配置（让前端能访问后端）
│   └── resources/
│       ├── application.properties      # 配置文件（数据库、端口等，端口 18081）
│       ├── data.sql                    # 初始测试数据
│       └── static/                     # npm run build 产物，后端直接托管
└── frontend/                       # 前端代码（Vue 3 + TS + antd，与后端分离）
    ├── package.json / vite.config.ts / tsconfig.json
    ├── index.html                      # Vite 入口页
    └── src/
        ├── main.ts                     # 入口：挂载 router
        ├── App.vue                     # 外壳：布局 + 侧边导航 + RouterView
        ├── router/index.ts             # hash 路由：/list、/operation-log
        ├── shared/                     # 跨页面共享
        │   └── use-permission-config.ts    # 按钮权限（静态演示）
        └── views/                      # 页面（每页自包含，自治）
            ├── list/                   # 待办列表页
            │   ├── api/
            │   │   ├── index.ts        # 接口怎么调（函数）
            │   │   └── type.ts         # 数据长什么样（类型）
            │   ├── constant/index.ts   # 状态筛选等固定值
            │   ├── hooks/
            │   │   ├── use-search.tsx        # 搜索栏配置
            │   │   └── use-table-columns.tsx # 表格列配置（JSX）
            │   ├── components/
            │   │   └── todo-form-drawer.vue  # 新增/编辑表单抽屉
            │   └── index.vue           # 组装层：搜索+表格+分页+抽屉
            └── operation-log/
                └── index.vue           # 操作日志（空壳占位页）
```

### 一个请求是怎么走完整个链路的？

以「新增待办」为例：

```
前端 POST /api/todos {title:"吃饭"}
        │
        ▼
TodoController.create()   ← 接口层：接收请求
        │
        ▼
TodoService.create()      ← 业务层：处理业务
        │
        ▼
TodoRepository.save()     ← 数据层：写进数据库
        │
        ▼
   返回新存的 Todo（自动转成 JSON）→ 前端（表格刷新）
```

---

## 三、怎么运行

### 第 1 步：启动后端

在本目录打开终端，执行（任选其一）：

```bash
# Windows (CMD / PowerShell)
.\mvnw.cmd spring-boot:run

# Git Bash / macOS / Linux
./mvnw spring-boot:run
```

> 第一次运行 `mvnw` 会自动下载 Maven 和依赖，需要联网，稍等一两分钟。
> 看到类似 `Started DemoApplication in x.xxx seconds` 就说明启动成功了。

后端地址：`http://localhost:18081`

### 第 2 步：启动前端

两种方式任选：

**开发模式（推荐，改代码保存即热更新）** —— 新开一个终端：

```bash
cd frontend
npm install        # 第一次需要
npm run dev
```

浏览器打开： **http://localhost:5173**
（`/api` 请求由 Vite 自动代理给 18081 后端，无需关心跨域。）

**正式模式（一个端口跑全栈）**：

```bash
cd frontend
npm run build      # 产物输出到 src/main/resources/static
```

然后（重新）启动后端，浏览器打开： **http://localhost:18081**（自动进入 `#/list`）。

### 第 3 步：玩起来

- 「新增待办」→ 打开抽屉表单，输入标题保存（POST 新增）
- 表格「状态」列 Switch → 切换完成状态（PUT 更新）
- 操作列「编辑」→ 抽屉里改标题/状态（PUT 更新）
- 操作列「删除」→ 确认气泡后删除（DELETE）
- 搜索栏关键词/状态 → 前端本地过滤；表格自带分页
- 侧边导航切到「操作日志」→ 空壳占位页（待实现）

---

## 四、接口一览

| 方法 | 地址 | 作用 | 请求体 |
|---|---|---|---|
| GET | `/api/todos` | 获取全部待办 | 无 |
| POST | `/api/todos` | 新增待办 | `{"title":"xxx"}` |
| PUT | `/api/todos/{id}` | 更新待办 | `{"title":"xxx","done":true}` |
| DELETE | `/api/todos/{id}` | 删除待办 | 无 |

可以直接用浏览器或 curl 试，例如：

```bash
curl http://localhost:18081/api/todos
curl -X POST http://localhost:18081/api/todos -H "Content-Type: application/json" -d "{\"title\":\"学习\"}"
```

---

## 五、看看数据库里的数据

当前默认用 **MySQL**（连接信息见 `application.properties`，会自动建库建表），
用任意 MySQL 客户端连 `localhost:3306`，查 `demo` 库的 `todo` 表即可。

想切回免安装的 H2 内存库：把 `application.properties` 里 H2 那 4 行的 `#` 去掉、
注释掉 MySQL 那组，重启即可；并可在浏览器打开 **http://localhost:18081/h2-console**
（JDBC URL 填 `jdbc:h2:mem:demo`，用户名 `sa`，密码留空）直接看表、手动执行 SQL。

---

## 六、几个常见疑问

**1. 为什么需要 CorsConfig？**
开发模式下前端在 `localhost:5173`、后端在 `localhost:18081`，端口不同属于「跨源」。
不过 Vite 已把 `/api` 代理到后端，请求实际同源；`CorsConfig` 是直连场景的兜底放行。

**2. 为什么用 hash 路由（地址带 `#`）？**
构建产物由 Spring Boot 静态托管，hash 路由刷新页面不会 404，后端不用加 SPA 转发配置。

**3. 改了代码怎么生效？**
后端：改完 Java 代码要重新运行 `mvnw spring-boot:run`（重启）。
前端：开发模式下保存即热更新；正式模式改完要重新 `npm run build`（并重启后端）。

---

## 七、下一步可以怎么改着玩

- 给 `TodoRepository` 加 `findByDone(boolean)` 等方法，把前端「状态筛选」改成走后端查询
- 给 operation-log 页补上真实的操作日志数据（后端加表 + 接口）
- 把 `use-permission-config.ts` 接上真实登录态，体验按钮级权限
- 给每个接口加参数校验、统一异常处理
