# 认证模块设计：登录、注册与找回密码

- 日期：2026-09-18
- 状态：设计已确认，待规格审阅
- 范围：现有 Spring Boot + Vue 3 待办项目

## 1. 目标与范围

为现有待办应用增加一套独立认证模块，包含：

- 邮箱 + 密码注册
- 邮箱 + 密码登录
- 退出登录
- 获取当前登录用户
- 找回密码
- 通过当前登录用户隔离待办数据
- 为已有无归属待办建立系统用户归属

第一版不包含：

- 邮箱注册验证
- 真实邮件发送
- 手机短信验证
- 管理员角色
- 多因素认证
- 社交登录

找回密码第一版使用固定演示验证码 `123456`，但接口和服务边界按未来接入真实邮箱验证码的方式设计。

## 2. 已确认的业务规则

### 2.1 账号

- 账号标识为邮箱。
- 邮箱格式由前端和后端共同校验。
- 保存前将邮箱规范化为小写并去除首尾空白。
- 邮箱具有唯一约束。
- 注册时检查邮箱是否已经存在。
- 如果已存在，返回明确的 `EMAIL_ALREADY_EXISTS` 结果；前端对用户统一提示“邮箱不可用”。
- 第一版注册不要求邮箱验证，注册成功后跳转登录页。

### 2.2 密码

密码至少 8 位，并且以下三类中至少满足两类：

1. 字母（`A-Z` 或 `a-z`）
2. 数字（`0-9`）
3. 符号

密码区分大小写；大写字母和小写字母不是两种独立组合。空格不允许。前端用于即时提示，后端是最终校验方。

数据库只保存 Argon2 哈希值，不保存明文密码。密码哈希和密码复杂度校验是两个独立职责。

### 2.3 登录状态

使用 Spring Security 的 Session + HttpOnly Cookie：

- 登录成功后由后端创建 Session。
- Session 标识通过 HttpOnly Cookie 保存，前端 JavaScript 不能读取。
- 前端不保存 JWT、密码或 Session ID 到 `localStorage`。
- 受保护的待办接口只信任后端当前认证用户，不接收或信任前端传入的 `userId`。
- 保留 CSRF 防护；前端请求封装负责携带 CSRF 请求头。

### 2.4 角色

第一版只有普通用户 `USER`。角色字段保留，为后续管理员角色扩展使用，但第一版不实现管理员菜单或管理员接口。

### 2.5 系统用户

已有待办数据归属不可登录的系统用户：

- 邮箱：`system@local`
- 角色：`USER`
- 状态：禁用，不允许登录
- 用途：只作为历史数据的归属者

启动时如果系统用户不存在则创建；已有 `user_id` 为空的待办归属到系统用户。新建待办必须归属当前登录用户。

## 3. 总体架构

### 3.1 前端目录

```text
frontend/src/
├─ modules/
│  └─ auth/
│     ├─ api/
│     │  ├─ index.ts
│     │  └─ type.ts
│     ├─ components/
│     │  ├─ auth-form.vue
│     │  └─ password-input.vue
│     ├─ constant/
│     │  └─ index.ts
│     ├─ hooks/
│     │  └─ use-auth.ts
│     └─ pages/
│        ├─ login/
│        │  └─ index.vue
│        ├─ register/
│        │  └─ index.vue
│        └─ forgot-password/
│           └─ index.vue
├─ layouts/
│  └─ admin-layout.vue
├─ router/
├─ shared/
└─ views/
   ├─ list/
   └─ operation-log/
```

`modules/auth` 只负责认证业务。`views` 保留现有后台业务页面。当前 `App.vue` 中的后台头部和侧边栏迁移到 `layouts/admin-layout.vue`，让登录、注册和找回密码不显示后台导航外壳。

路由页面使用 `pages`，现有后台路由页面继续使用 `views`；两者不在同一目录层级混用。

### 3.2 后端目录

```text
src/main/java/com/example/demo/
├─ auth/
│  ├─ controller/
│  │  └─ AuthController.java
│  ├─ dto/
│  │  ├─ RegisterRequest.java
│  │  ├─ LoginRequest.java
│  │  ├─ ResetCodeRequest.java
│  │  ├─ ResetPasswordRequest.java
│  │  └─ AuthUserResponse.java
│  ├─ entity/
│  │  └─ User.java
│  ├─ repository/
│  │  └─ UserRepository.java
│  ├─ service/
│  │  ├─ AuthService.java
│  │  └─ PasswordResetService.java
│  └─ security/
│     ├─ SecurityConfig.java
│     └─ CustomUserDetailsService.java
├─ todo/
│  └─ ...
└─ config/
   └─ ...
```

当前待办类仍可先留在既有 `entity`、`repository`、`service`、`controller` 包中；认证模块作为独立业务边界新增。认证模块不直接暴露 `User` JPA 实体给前端，只返回安全的用户响应 DTO。

## 4. 数据模型

### 4.1 User

```text
id             Long，主键
email          String，唯一、规范化为小写
passwordHash   String，Argon2 哈希
role           String/枚举，第一版为 USER
enabled        boolean，系统用户为 false
createdAt      时间
```

注册时只写入 `passwordHash`。密码、确认密码和密码重置验证码不落库为明文。

### 4.2 Todo

现有 Todo 增加所属用户关系：

```text
user_id        User 外键，应用层必须非空
```

`TodoService` 的查询、新增、更新、删除都必须基于当前认证用户：

- 查询：只查询当前用户的待办
- 新增：自动设置当前用户
- 更新：按 `id + 当前用户` 查找
- 删除：按 `id + 当前用户` 删除

不能通过 URL、请求体或查询参数让客户端指定另一个用户。

## 5. API 设计

认证 API 前缀为 `/api/auth`。

### 5.1 注册

```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Abc12345",
  "confirmPassword": "Abc12345"
}
```

成功：返回用户基本信息，不自动创建登录 Session，前端显示成功提示并跳转 `/login`。

邮箱重复：HTTP `409`，错误码 `EMAIL_ALREADY_EXISTS`；前端不展示内部错误码，统一提示“邮箱不可用”。

参数错误：HTTP `400`，错误码可包括：

- `INVALID_EMAIL`
- `PASSWORD_POLICY_INVALID`
- `PASSWORD_CONFIRMATION_MISMATCH`

### 5.2 登录

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Abc12345"
}
```

成功：创建 Session，并返回用户基本信息。

认证失败统一返回 `INVALID_CREDENTIALS`，避免通过登录错误信息区分邮箱是否存在。

禁用用户不能登录，系统用户因此不能通过登录接口使用。

### 5.3 当前用户

```http
GET /api/auth/me
```

已登录返回用户基本信息；未登录返回 HTTP `401`。

### 5.4 退出登录

```http
POST /api/auth/logout
```

使当前 Session 失效并清理 Cookie，成功返回 `204` 或统一成功响应。

### 5.5 获取演示重置验证码

```http
POST /api/auth/password/reset-code
Content-Type: application/json

{
  "email": "user@example.com"
}
```

第一版不发邮件，后端只执行演示流程。固定验证码由重置服务校验为 `123456`。接口不把真实密码或用户数据返回给前端；找回流程可以对不存在邮箱返回通用提示。

未来替换为：生成随机验证码、保存过期时间、发送邮箱、限制尝试次数。

### 5.6 重置密码

```http
POST /api/auth/password/reset
Content-Type: application/json

{
  "email": "user@example.com",
  "code": "123456",
  "newPassword": "New12345",
  "confirmPassword": "New12345"
}
```

后端校验验证码、密码规则和两次密码一致后，用 Argon2 生成新哈希并更新用户密码。重置成功后不自动登录，前端跳转登录页。

## 6. 前端路由与页面行为

公开路由：

```text
/login
/register
/forgot-password
```

受保护路由：

```text
/list
/operation-log
```

路由元信息标记 `requiresAuth`。路由守卫通过 `/api/auth/me` 或内存中的当前用户状态判断：

- 未登录访问受保护页面：跳转 `/login`
- 已登录访问登录、注册页面：跳转 `/list`
- 登录成功：跳转 `/list`
- 退出成功：跳转 `/login`

`App.vue` 只保留根级 `RouterView`；后台布局由受保护路由使用 `admin-layout.vue` 提供。

前端 API 层统一处理：

- JSON 请求头
- Cookie 携带
- HTTP 状态码和业务错误码
- 表单错误展示
- 登录过期时跳转登录页

## 7. 安全与错误处理

- 使用 `Argon2PasswordEncoder`，禁止明文密码和可逆加密。
- 密码规则在前后端都校验，后端校验不可省略。
- 登录接口不区分“邮箱不存在”和“密码错误”。
- 认证失败、未登录访问和权限不足使用明确但不泄露敏感信息的错误码。
- 使用 HttpOnly、适当 SameSite 属性的 Session Cookie。
- 使用 Spring Security CSRF 防护保护基于 Cookie 的写操作。
- 前端不记录密码、验证码和 Session 信息到日志。
- 注册邮箱是否存在会按需求反馈，因此该接口允许暴露“邮箱已注册”状态；密码找回接口使用通用反馈，避免额外暴露账号信息。
- 待办资源访问必须在 Service/Repository 查询条件中绑定当前用户，不能只依赖前端页面隐藏。

## 8. 迁移与初始化

1. 增加 `users` 表和 User 实体。
2. 创建或查找系统用户 `system@local`。
3. 为现有 Todo 增加用户关联字段。
4. 将所有历史无归属 Todo 绑定到系统用户。
5. 新增 Todo 时强制设置当前认证用户。
6. 现有 `data.sql` 不再直接假设 Todo 没有用户字段；初始化逻辑负责系统用户和历史数据归属。

当前项目使用 JPA `ddl-auto=update`，首次迁移应保持对已有数据的兼容：用户关联字段在历史数据回填完成前允许为空，应用初始化完成后业务层禁止创建空归属数据。后续进入正式部署时可再用数据库迁移工具增加非空约束。

## 9. 测试范围

### 后端

- 邮箱格式校验
- 密码长度和两类组合校验
- 密码大小写和空格规则
- 重复邮箱返回 `EMAIL_ALREADY_EXISTS`
- Argon2 哈希不能还原明文
- 正确登录创建 Session
- 错误密码不能登录
- 禁用系统用户不能登录
- 登出后 Session 失效
- 未登录不能访问 Todo API
- 用户 A 不能读取、修改、删除用户 B 的 Todo
- 用户只能查询自己的 Todo
- 固定验证码成功和失败场景
- 历史 Todo 正确归属系统用户

### 前端

- 注册表单校验和重复邮箱反馈
- 登录成功、失败和跳转
- 找回密码表单校验
- 固定验证码流程
- 路由守卫
- 未登录时不显示后台布局
- 退出后回到登录页

### 手工验收

1. 注册新邮箱并成功跳转登录。
2. 使用相同邮箱再次注册，看到已注册反馈。
3. 登录后只能看到该用户自己的待办。
4. 新增、编辑、完成状态切换和删除均只影响当前用户数据。
5. 使用 `123456` 找回密码并以新密码登录。
6. 旧数据属于系统用户且系统用户无法登录。

## 10. 后续扩展边界

接入真实邮箱验证时，只替换验证码服务实现和增加验证状态，不改变登录、注册页面的总体边界。届时增加：

- `emailVerified`
- 随机验证码和过期时间
- 邮箱发送适配器
- 发送频率限制
- 验证失败次数限制

管理员功能后续增加角色枚举、后端授权规则和管理员页面，不改变普通用户的 Todo 所有权模型。
