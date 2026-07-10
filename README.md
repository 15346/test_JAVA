# 极简前后端分离示例（Spring Boot + 原生 JS）

一个**待办清单（Todo）**小项目，用来学习「前后端分离」最核心的东西：
后端用 Spring Boot 提供 REST 接口，前端用最朴素的原生 HTML + JavaScript 调用接口，
覆盖 **增、删、改、查（CRUD）** 四种操作。

> 故意写得特别简单：没有 React/Vue，没有复杂配置，只有最基础的四层结构和 fetch 调用，
> 目的是让你一眼看懂「一个请求从前端到数据库再回来」的完整链路。

---

## 一、技术栈

| 层 | 技术 | 作用 |
|---|---|---|
| 后端框架 | Spring Boot 4 | 快速搭建 Web 服务 |
| 数据库 | H2（内存数据库） | 不用安装，启动即用，重启清空 |
| 数据访问 | Spring Data JPA | 几乎不用写 SQL 就能增删改查 |
| 前端 | 原生 HTML / CSS / JavaScript | 不依赖任何框架，便于理解 |

环境要求：**JDK 17 及以上**（本项目用 21）。

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
│       ├── application.properties      # 配置文件（数据库、端口等）
│       └── data.sql                    # 初始测试数据
└── frontend/                       # 前端代码（与后端分离）
    ├── index.html
    ├── app.js
    └── style.css
```

### 一个请求是怎么走完整个后端的？

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
   返回新存的 Todo（自动转成 JSON）→ 前端
```

这就是经典的 **Controller → Service → Repository → 数据库** 四层结构。

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

后端地址：`http://localhost:8081`

### 第 2 步：启动前端

前端只是几个静态文件，需要一个简单的本地服务器来打开（**不能直接双击 index.html**，否则会因跨域/协议问题调不通接口）。

新开一个终端，进入 `frontend` 目录：

```bash
cd frontend

# 方式 A：用 Python（Windows 上一般都有）
python -m http.server 8000

# 方式 B：用 Node
npx serve -l 8000
```

然后浏览器打开： **http://localhost:8000**

### 第 3 步：玩起来

- 在输入框输入文字 → 回车或点「添加」（POST 新增）
- 勾选复选框 → 切换完成状态（PUT 更新）
- 点「删除」→ 删除（DELETE）
- 刷新页面 → 重新拉取列表（GET 查询）

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
curl http://localhost:8081/api/todos
curl -X POST http://localhost:8081/api/todos -H "Content-Type: application/json" -d "{\"title\":\"学习\"}"
```

---

## 五、看看数据库里的数据

启动后端后，浏览器打开： **http://localhost:8081/h2-console**

填好连接信息：

- JDBC URL：`jdbc:h2:mem:demo`
- User Name：`sa`
- Password：（留空）

点 Connect，就能看到 `TODO` 表和里面的数据，还可以手动执行 SQL。

---

## 六、几个常见疑问

**1. 为什么需要 CorsConfig？**
前端在 `localhost:8000`、后端在 `localhost:8081`，端口不同属于「跨源」，
浏览器默认会拦截。`CorsConfig` 就是放行这些来自本地的请求。

**2. 为什么用内存数据库？**
不用安装、不用配置，启动即用，最适合学习。代价是重启后数据没了。
想换成 MySQL/PostgreSQL，只需改 `application.properties` 里的连接信息和依赖。

**3. 改了代码怎么生效？**
后端：改完 Java 代码要重新运行 `mvnw spring-boot:run`（重启）。
前端：改完 HTML/JS/CSS，刷新浏览器即可。

---

## 七、下一步可以怎么改着玩

- 给 `TodoRepository` 加 `findByDone(boolean)` 等方法，做个「只看未完成」按钮
- 加分页、排序
- 把前端换成 Vue 或 React
- 给每个接口加参数校验、统一异常处理
