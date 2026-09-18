# Authentication Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为现有 Spring Boot + Vue 3 待办应用增加邮箱注册、登录、退出登录、固定验证码找回密码，并让每个用户只能访问自己的待办数据。

**Architecture:** 后端使用 Spring Security 的 Session + HttpOnly Cookie，用户密码使用 Argon2 哈希；认证接口只返回安全的用户 DTO。Todo 通过 `user_id` 关联用户，所有查询和写操作都从当前认证主体取得用户，不接受客户端传入的 `userId`。前端新增 `modules/auth`，使用公开认证页面和受保护的后台布局，通过路由守卫调用当前用户接口。

**Tech Stack:** Java 21、Spring Boot 4.1、Spring Security、Spring Data JPA、MySQL/H2、Argon2；Vue 3、TypeScript、Vue Router、ant-design-vue、Vite、Vitest。

**Spec:** [认证模块设计](../specs/2026-09-18-auth-module-design.md)

## Global Constraints

- 邮箱保存前必须去除首尾空白并使用 `Locale.ROOT` 规范化为小写；数据库唯一约束作用于规范化后的邮箱。
- 重复注册的后端错误码为 `EMAIL_ALREADY_EXISTS`，前端给用户的唯一提示为“邮箱不可用”。
- 密码至少 8 位，必须包含字母、数字、符号三类中的至少两类；字母大小写区分，但大小写不算两种独立组合；空格不允许。
- 密码只能保存 Argon2 哈希，任何日志、响应和 DTO 都不能包含明文密码。
- 第一版找回密码固定使用验证码 `123456`，不发送邮件；服务边界必须允许未来替换为邮件验证码实现。
- 第一版只有 `USER` 角色；`system@local` 为 `enabled=false` 的历史数据归属用户，不能登录。
- Session 使用 HttpOnly Cookie；写操作保留 CSRF 防护，前端请求封装必须带 CSRF 请求头。
- Todo 的 API 不接受 `userId`；当前用户由 Spring Security 认证主体确定。
- 登录、注册、重置成功与失败消息使用中文；登录失败统一提示“邮箱或密码错误”。
- 现有 `src/main/resources/application.properties` 中的 MySQL 配置和已有注释修改不得被回退。
- 除非用户明确要求，不在执行计划过程中提交或推送 Git commit；每个任务以测试结果作为检查点。

## File Map

### Backend files

- Modify `pom.xml`: 加入 Spring Security、Validation 及测试依赖。
- Modify `src/main/resources/application.properties`: 增加认证和 Session/CSRF 所需配置，不修改现有数据库凭据。
- Create `src/test/resources/application.properties`: 测试使用 H2 的独立配置。
- Create `src/main/java/com/example/demo/auth/entity/User.java`: 用户 JPA 实体。
- Create `src/main/java/com/example/demo/auth/entity/Role.java`: `USER` 角色枚举。
- Create `src/main/java/com/example/demo/auth/repository/UserRepository.java`: 邮箱查找和唯一性查询。
- Create `src/main/java/com/example/demo/auth/dto/RegisterRequest.java`: 注册请求 DTO。
- Create `src/main/java/com/example/demo/auth/dto/LoginRequest.java`: 登录请求 DTO。
- Create `src/main/java/com/example/demo/auth/dto/ResetCodeRequest.java`: 请求演示验证码 DTO。
- Create `src/main/java/com/example/demo/auth/dto/ResetPasswordRequest.java`: 重置密码 DTO。
- Create `src/main/java/com/example/demo/auth/dto/AuthUserResponse.java`: 对外安全用户 DTO。
- Create `src/main/java/com/example/demo/auth/service/EmailNormalizer.java`: 邮箱规范化。
- Create `src/main/java/com/example/demo/auth/service/PasswordPolicy.java`: 密码规则校验。
- Create `src/main/java/com/example/demo/auth/service/AuthService.java`: 注册和用户查找业务。
- Create `src/main/java/com/example/demo/auth/service/PasswordResetService.java`: 固定验证码和密码重置业务边界。
- Create `src/main/java/com/example/demo/auth/security/UserPrincipal.java`: Spring Security principal。
- Create `src/main/java/com/example/demo/auth/security/CustomUserDetailsService.java`: 按规范化邮箱加载用户。
- Create `src/main/java/com/example/demo/auth/security/SecurityConfig.java`: 密码编码器、认证管理器、Session、CSRF 和 URL 授权。
- Create `src/main/java/com/example/demo/auth/controller/AuthController.java`: 注册、登录、当前用户、CSRF 和密码重置 HTTP 接口；退出由 Spring Security LogoutFilter 处理。
- Create `src/main/java/com/example/demo/config/ApiError.java`: 统一错误响应。
- Create `src/main/java/com/example/demo/config/ApiException.java`: 带 HTTP 状态和业务码的异常。
- Create `src/main/java/com/example/demo/config/GlobalExceptionHandler.java`: 参数、认证和业务异常映射。
- Modify `src/main/java/com/example/demo/entity/Todo.java`: 增加用户关联。
- Modify `src/main/java/com/example/demo/repository/TodoRepository.java`: 增加按用户查询的方法。
- Modify `src/main/java/com/example/demo/service/TodoService.java`: 所有操作绑定当前用户。
- Modify `src/main/java/com/example/demo/controller/TodoController.java`: 使用认证主体和 Todo DTO。
- Create `src/main/java/com/example/demo/dto/TodoRequest.java`: 不含 `userId` 的待办输入 DTO。
- Create `src/main/java/com/example/demo/dto/TodoResponse.java`: 不暴露 JPA 关联实体的待办响应 DTO。
- Create `src/main/java/com/example/demo/config/SystemUserInitializer.java`: 创建系统用户并迁移无归属旧待办。
- Modify `src/main/resources/data.sql`: 保持演示待办数据可插入，并更新注释说明由初始化器补充系统用户归属。

### Frontend files

- Create `frontend/src/shared/http/request.ts`: Cookie、CSRF、JSON、业务错误统一请求封装。
- Create `frontend/src/shared/http/request.test.ts`: 请求封装测试。
- Create `frontend/src/modules/auth/api/type.ts`: 认证请求和响应类型。
- Create `frontend/src/modules/auth/api/index.ts`: 注册、登录、退出、当前用户、重置密码 API。
- Create `frontend/src/modules/auth/api/index.test.ts`: 认证 API 请求测试。
- Create `frontend/src/modules/auth/constant/index.ts`: 演示验证码和文案常量。
- Create `frontend/src/modules/auth/hooks/use-auth.ts`: 当前用户状态和认证操作。
- Create `frontend/src/modules/auth/hooks/use-auth.test.ts`: 当前用户状态测试。
- Create `frontend/src/modules/auth/components/password-input.vue`: 密码输入和规则提示组件。
- Create `frontend/src/modules/auth/pages/login/index.vue`: 登录页。
- Create `frontend/src/modules/auth/pages/register/index.vue`: 注册页。
- Create `frontend/src/modules/auth/pages/forgot-password/index.vue`: 找回密码页。
- Create `frontend/src/layouts/admin-layout.vue`: 登录后的后台头部、导航和退出登录。
- Modify `frontend/src/App.vue`: 只保留根级 `RouterView`。
- Modify `frontend/src/router/index.ts`: 增加公开路由、受保护嵌套路由和路由守卫。
- Modify `frontend/src/views/list/api/index.ts`: 改用统一请求封装。
- Modify `frontend/src/views/list/api/index.test.ts`: 适配统一请求封装和 Cookie/CSRF 请求选项。
- Modify `frontend/src/views/list/index.vue`: 处理认证过期和错误反馈，不改变现有搜索表格行为。

---

### Task 1: 建立后端认证基础依赖、测试配置和纯规则组件

**Files:**
- Modify: `pom.xml`
- Create: `src/test/resources/application.properties`
- Create: `src/main/java/com/example/demo/auth/service/EmailNormalizer.java`
- Create: `src/main/java/com/example/demo/auth/service/PasswordPolicy.java`
- Create: `src/test/java/com/example/demo/auth/service/EmailNormalizerTest.java`
- Create: `src/test/java/com/example/demo/auth/service/PasswordPolicyTest.java`

**Interfaces:**
- Produces `EmailNormalizer.normalize(String): String`，空值返回 `null`，非空值执行 `trim().toLowerCase(Locale.ROOT)`。
- Produces `PasswordPolicy.isValid(String): boolean`。
- `PasswordPolicy.isValid` 对 `null`、少于 8 位、含空格或组合类别少于两类返回 `false`。

- [ ] **Step 1: 增加依赖和测试 profile 配置**

在 `pom.xml` 增加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.80</version>
</dependency>
```

创建 `src/test/resources/application.properties`：

```properties
spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.sql.init.mode=never
spring.h2.console.enabled=false
```

- [ ] **Step 2: 写邮箱规范化失败测试并运行**

```java
@Test
void normalizeTrimsAndLowercasesUsingRootLocale() {
    assertEquals("hello@126.com", EmailNormalizer.normalize("  Hello@126.COM "));
}

@Test
void normalizeReturnsNullForNull() {
    assertNull(EmailNormalizer.normalize(null));
}
```

运行：

```bash
./mvnw -q -Dtest=EmailNormalizerTest test
```

预期：失败，因为 `EmailNormalizer` 尚未实现。

- [ ] **Step 3: 实现邮箱规范化**

```java
public final class EmailNormalizer {
    private EmailNormalizer() {}

    public static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
```

- [ ] **Step 4: 写密码策略测试并运行**

测试必须覆盖边界和全部组合：

```java
@Test
void acceptsEightCharactersWithLetterAndDigit() {
    assertTrue(PasswordPolicy.isValid("abc12345"));
}

@Test
void acceptsLetterAndSymbol() {
    assertTrue(PasswordPolicy.isValid("abcdefgh!"));
}

@Test
void acceptsDigitAndSymbol() {
    assertTrue(PasswordPolicy.isValid("1234567!"));
}

@Test
void rejectsOnlyLettersOnlyDigitsAndOnlySymbols() {
    assertAll(
        () -> assertFalse(PasswordPolicy.isValid("abcdefgh")),
        () -> assertFalse(PasswordPolicy.isValid("12345678")),
        () -> assertFalse(PasswordPolicy.isValid("!@#$%^&*"))
    );
}

@Test
void rejectsShortPasswordsWhitespaceAndNull() {
    assertAll(
        () -> assertFalse(PasswordPolicy.isValid("a1!")),
        () -> assertFalse(PasswordPolicy.isValid("abc 1234")),
        () -> assertFalse(PasswordPolicy.isValid(null))
    );
}

@Test
void uppercaseAndLowercaseAreOneLetterCategory() {
    assertFalse(PasswordPolicy.isValid("Abcdefgh"));
}
```

运行：

```bash
./mvnw -q -Dtest=PasswordPolicyTest test
```

预期：失败，因为 `PasswordPolicy` 尚未实现。

- [ ] **Step 5: 实现密码策略**

使用三类布尔值，不把大写和小写拆成两类：

```java
public final class PasswordPolicy {
    private PasswordPolicy() {}

    public static boolean isValid(String password) {
        if (password == null || password.length() < 8
                || password.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        boolean letter = password.matches(".*[A-Za-z].*");
        boolean digit = password.matches(".*[0-9].*");
        boolean symbol = password.matches(".*[^\\sA-Za-z0-9].*");
        int categories = (letter ? 1 : 0) + (digit ? 1 : 0) + (symbol ? 1 : 0);
        return categories >= 2;
    }
}
```

- [ ] **Step 6: 运行规则测试和后端编译**

```bash
./mvnw -q -Dtest=EmailNormalizerTest,PasswordPolicyTest test
./mvnw -q -DskipTests compile
```

预期：规则测试全部通过，编译成功。

---

### Task 2: 建立用户实体、DTO 和认证业务服务

**Files:**
- Create: `src/main/java/com/example/demo/auth/entity/Role.java`
- Create: `src/main/java/com/example/demo/auth/entity/User.java`
- Create: `src/main/java/com/example/demo/auth/repository/UserRepository.java`
- Create: `src/main/java/com/example/demo/auth/dto/RegisterRequest.java`
- Create: `src/main/java/com/example/demo/auth/dto/LoginRequest.java`
- Create: `src/main/java/com/example/demo/auth/dto/ResetCodeRequest.java`
- Create: `src/main/java/com/example/demo/auth/dto/ResetPasswordRequest.java`
- Create: `src/main/java/com/example/demo/auth/dto/AuthUserResponse.java`
- Create: `src/main/java/com/example/demo/auth/service/AuthService.java`
- Create: `src/test/java/com/example/demo/auth/service/AuthServiceTest.java`
- Create: `src/test/java/com/example/demo/auth/repository/UserRepositoryTest.java`

**Interfaces:**
- `UserRepository.findByEmail(String email): Optional<User>`。
- `UserRepository.existsByEmail(String email): boolean`。
- `AuthService.register(RegisterRequest request): AuthUserResponse`。
- `AuthService.findByEmail(String rawEmail): Optional<User>`。

- [ ] **Step 1: 写用户实体和 Repository 的失败测试**

```java
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {
    @Autowired UserRepository repository;

    @Test
    void findsUserByNormalizedEmail() {
        User user = User.create("hello@126.com", "hash", Role.USER, true);
        repository.save(user);

        assertTrue(repository.findByEmail("hello@126.com").isPresent());
        assertTrue(repository.existsByEmail("hello@126.com"));
    }
}
```

运行：

```bash
./mvnw -q -Dtest=UserRepositoryTest test
```

预期：失败，因为 User 实体和 Repository 尚未创建。

- [ ] **Step 2: 实现 Role、User 和 UserRepository**

`User` 使用表名 `users`，避免使用数据库保留字；字段为 `id`、`email`、`passwordHash`、`role`、`enabled`、`createdAt`。邮箱列增加唯一约束。提供 JPA 无参构造器和仅接收已规范化值的工厂方法：

```java
public static User create(String normalizedEmail, String passwordHash,
                          Role role, boolean enabled) {
    User user = new User();
    user.email = normalizedEmail;
    user.passwordHash = passwordHash;
    user.role = role;
    user.enabled = enabled;
    user.createdAt = Instant.now();
    return user;
}
```

Repository：

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 3: 写注册业务失败测试**

使用 Mockito mock `UserRepository` 和 `PasswordEncoder`，覆盖：

```java
@Test
void registerNormalizesEmailAndStoresOnlyEncodedPassword() {
    when(repository.existsByEmail("hello@126.com")).thenReturn(false);
    when(encoder.encode("abc12345")).thenReturn("argon2-hash");
    when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    AuthUserResponse result = service.register(
        new RegisterRequest(" Hello@126.COM ", "abc12345", "abc12345")
    );

    verify(encoder).encode("abc12345");
    verify(repository).save(argThat(user ->
        user.getEmail().equals("hello@126.com")
            && user.getPasswordHash().equals("argon2-hash")
            && user.getRole() == Role.USER
    ));
    assertEquals("hello@126.com", result.email());
}

@Test
void duplicateEmailThrowsEmailAlreadyExists() {
    when(repository.existsByEmail("hello@126.com")).thenReturn(true);

    ApiException exception = assertThrows(ApiException.class, () ->
        service.register(new RegisterRequest(
            "Hello@126.com", "abc12345", "abc12345"
        ))
    );

    assertEquals("EMAIL_ALREADY_EXISTS", exception.code());
    verify(repository, never()).save(any());
}
```

- [ ] **Step 4: 实现 DTO 和 AuthService 注册逻辑**

`AuthService.register` 按顺序执行：规范化邮箱、校验邮箱格式、校验两次密码一致、校验 `PasswordPolicy`、检查 `existsByEmail`、使用 `PasswordEncoder.encode`、保存 `Role.USER/enabled=true` 用户。重复邮箱抛出 `ApiException(409, "EMAIL_ALREADY_EXISTS", "邮箱不可用")`。

邮箱格式使用明确的后端规则，例如：

```java
private static final Pattern EMAIL =
    Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
```

`AuthUserResponse` 只能包含 `id`、`email`、`role`，不能包含 `passwordHash`、密码或内部 JPA 关系。

- [ ] **Step 5: 运行服务测试**

```bash
./mvnw -q -Dtest=AuthServiceTest,UserRepositoryTest test
```

预期：注册成功、重复邮箱、密码规则和 Repository 测试全部通过。

---

### Task 3: 配置 Spring Security、Session、CSRF 和统一错误响应

**Files:**
- Create: `src/main/java/com/example/demo/auth/security/UserPrincipal.java`
- Create: `src/main/java/com/example/demo/auth/security/CustomUserDetailsService.java`
- Create: `src/main/java/com/example/demo/auth/security/SecurityConfig.java`
- Create: `src/main/java/com/example/demo/config/ApiError.java`
- Create: `src/main/java/com/example/demo/config/ApiException.java`
- Create: `src/main/java/com/example/demo/config/GlobalExceptionHandler.java`
- Modify: `src/main/resources/application.properties`
- Create: `src/test/java/com/example/demo/auth/security/SecurityConfigTest.java`

**Interfaces:**
- `CustomUserDetailsService.loadUserByUsername(String rawEmail): UserPrincipal`，内部先规范化邮箱。
- `SecurityConfig.passwordEncoder(): PasswordEncoder`，返回 `Argon2PasswordEncoder`。
- `SecurityConfig.authenticationManager(...): AuthenticationManager`。
- `ApiError(code, message)` 为统一 JSON 错误体。

- [ ] **Step 1: 写安全配置失败测试**

使用 MockMvc 覆盖匿名访问规则：

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {
    @Autowired MockMvc mvc;

    @Test
    void todoRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/todos"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void authRegisterEndpointIsPublic() throws Exception {
        mvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@example.com\",\"password\":\"abc12345\",\"confirmPassword\":\"abc12345\"}"))
            .andExpect(status().is2xxSuccessful());
    }
}
```

运行：

```bash
./mvnw -q -Dtest=SecurityConfigTest test
```

预期：失败，因为 SecurityConfig、UserDetailsService 和认证接口尚未存在。

- [ ] **Step 2: 实现 UserPrincipal 和 UserDetailsService**

`UserPrincipal` 持有 `User`，实现 `UserDetails`：

- `getUsername()` 返回规范化邮箱。
- `getPassword()` 返回 Argon2 哈希。
- `getAuthorities()` 返回 `ROLE_USER`。
- `isEnabled()` 使用 User.enabled。

`CustomUserDetailsService` 调用 `EmailNormalizer.normalize` 后执行 `repository.findByEmail`；找不到时抛出 `UsernameNotFoundException`。

- [ ] **Step 3: 实现 SecurityConfig**

配置以下行为：

```java
http
    .csrf(csrf -> csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/index.html", "/assets/**").permitAll()
        .requestMatchers("/api/auth/register", "/api/auth/login",
                         "/api/auth/csrf", "/api/auth/password/reset-code",
                         "/api/auth/password/reset").permitAll()
        .requestMatchers("/api/auth/me", "/api/auth/logout", "/api/todos/**")
            .authenticated()
        .anyRequest().permitAll())
    .sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
    .logout(logout -> logout
        .logoutUrl("/api/auth/logout")
        .invalidateHttpSession(true)
        .deleteCookies("JSESSIONID"));
```

创建 `Argon2PasswordEncoder` Bean，并从 `DaoAuthenticationProvider` 构建 `AuthenticationManager`。为手动 JSON 登录注入 `SecurityContextRepository`，使用 `HttpSessionSecurityContextRepository` 保存认证上下文。

- [ ] **Step 4: 实现统一错误响应**

`ApiException` 包含 `HttpStatus status`、`String code`、`String message`。`GlobalExceptionHandler` 映射：

- `ApiException` → 对应状态和错误码
- `MethodArgumentNotValidException` → `VALIDATION_ERROR`、字段消息
- 未认证 → `AUTH_REQUIRED`
- `AuthenticationException` → `INVALID_CREDENTIALS`
- 未处理异常 → `INTERNAL_ERROR`，日志中记录异常但响应不带堆栈

- [ ] **Step 5: 配置 Session Cookie 属性并运行安全测试**

在 `application.properties` 增加与现有数据库配置分开的认证配置：

```properties
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=lax
server.servlet.session.cookie.path=/
```

运行：

```bash
./mvnw -q -Dtest=SecurityConfigTest test
```

预期：匿名 Todo 请求为 `401`，公开认证端点可访问，安全测试通过。

---

### Task 4: 实现认证 Controller、登录/退出和固定验证码找回密码

**Files:**
- Create: `src/main/java/com/example/demo/auth/service/PasswordResetService.java`
- Create: `src/main/java/com/example/demo/auth/controller/AuthController.java`
- Create: `src/test/java/com/example/demo/auth/controller/AuthControllerTest.java`

**Interfaces:**
- `PasswordResetService.requestCode(String rawEmail): void`：第一版不发邮件，执行通用响应流程。
- `PasswordResetService.verify(String rawEmail, String code): User`：仅接受 `123456`，找不到邮箱或验证码错误时抛出统一重置错误。
- `PasswordResetService.resetPassword(ResetPasswordRequest request): void`：验证固定验证码和新密码后使用 Argon2 更新哈希。
- `AuthController` 提供 `/api/auth/register`、`/login`、`/me`、`/csrf`、`/password/reset-code`、`/password/reset`；`/logout` 由 Spring Security LogoutFilter 处理。

- [ ] **Step 1: 写 Controller 集成测试**

覆盖下列场景：

```java
@Test
void duplicateRegistrationReturnsConflictWithInternalCodeAndPublicMessage() throws Exception {
    mvc.perform(post("/api/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"existing@example.com\",\"password\":\"abc12345\",\"confirmPassword\":\"abc12345\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
        .andExpect(jsonPath("$.message").value("邮箱不可用"));
}

@Test
void loginCreatesSessionAndMeReturnsUser() throws Exception {
    mvc.perform(post("/api/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"user@example.com\",\"password\":\"abc12345\"}"))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("JSESSIONID"));
}

@Test
void wrongPasswordUsesGenericCredentialMessage() throws Exception {
    mvc.perform(post("/api/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"user@example.com\",\"password\":\"wrong123\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
        .andExpect(jsonPath("$.message").value("邮箱或密码错误"));
}

@Test
void resetAcceptsOnlyDemoCodeAndNewPasswordPolicy() throws Exception {
    mvc.perform(post("/api/auth/password/reset")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"user@example.com\",\"code\":\"123456\",\"newPassword\":\"new12345\",\"confirmPassword\":\"new12345\"}"))
        .andExpect(status().isNoContent());
}
```

使用 `@Sql` 或测试初始化器创建测试用户，密码必须通过同一个 `PasswordEncoder` 生成哈希；测试中不能直接保存明文到 `passwordHash`。

- [ ] **Step 2: 实现 PasswordResetService**

将固定值集中在服务常量或配置中：

```java
private static final String DEMO_CODE = "123456";
```

`requestCode` 对不存在邮箱也返回成功，避免额外暴露账号存在状态。`verify` 先规范化邮箱，再检查代码和用户是否存在，成功返回用户；失败使用 `RESET_CODE_INVALID`。

- [ ] **Step 3: 实现 JSON 登录**

Controller 使用 `AuthenticationManager.authenticate`：

```java
Authentication authentication = authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(
        EmailNormalizer.normalize(request.email()), request.password()));
SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(authentication);
SecurityContextHolder.setContext(context);
securityContextRepository.saveContext(context, httpRequest, httpResponse);
```

登录成功返回 `AuthUserResponse`。不要在响应中返回密码、哈希或 Session ID。

- [ ] **Step 4: 实现注册、当前用户、CSRF 和退出接口**

- 注册调用 `AuthService.register`，成功返回 `201` 和用户 DTO，不自动登录。
- `GET /api/auth/me` 使用 `@AuthenticationPrincipal UserPrincipal` 返回 DTO。
- `GET /api/auth/csrf` 接收 `CsrfToken` 参数并返回 `204`，迫使 CSRF Cookie 初始化。
- 退出使用 SecurityContext 的 logout 机制，成功返回 `204`。
- 重置密码验证固定代码、重复密码和密码规则，Argon2 编码新密码后返回 `204`，不自动创建 Session。

- [ ] **Step 5: 运行完整认证测试**

```bash
./mvnw -q -Dtest=AuthServiceTest,UserRepositoryTest,SecurityConfigTest,AuthControllerTest test
```

预期：注册、重复邮箱、登录、Session、`/me`、登出、CSRF、固定验证码和重置密码测试全部通过。

---

### Task 5: 为 Todo 增加用户归属并迁移系统用户数据

**Files:**
- Modify: `src/main/java/com/example/demo/entity/Todo.java`
- Modify: `src/main/java/com/example/demo/repository/TodoRepository.java`
- Modify: `src/main/java/com/example/demo/service/TodoService.java`
- Modify: `src/main/java/com/example/demo/controller/TodoController.java`
- Create: `src/main/java/com/example/demo/dto/TodoRequest.java`
- Create: `src/main/java/com/example/demo/dto/TodoResponse.java`
- Create: `src/main/java/com/example/demo/config/SystemUserInitializer.java`
- Modify: `src/main/resources/data.sql`
- Create: `src/test/java/com/example/demo/service/TodoOwnershipTest.java`
- Create: `src/test/java/com/example/demo/config/SystemUserInitializerTest.java`

**Interfaces:**
- `TodoRepository.findByUserId(Long userId): List<Todo>`。
- `TodoRepository.findByIdAndUserId(Long id, Long userId): Optional<Todo>`。
- `TodoRepository.findByUserIsNull(): List<Todo>`。
- `TodoService.findAll(User user): List<TodoResponse>`。
- `TodoService.create(User user, String title): TodoResponse`。
- `TodoService.update(User user, Long id, TodoRequest request): TodoResponse`。
- `TodoService.delete(User user, Long id): void`。
- `TodoRequest` 只有 `title`、`done`；没有 `userId`。
- `TodoResponse` 只有 `id`、`title`、`done`。

- [ ] **Step 1: 写用户隔离失败测试**

使用两个用户和两条 Todo，覆盖：

```java
@Test
void findAllReturnsOnlyCurrentUsersTodos() {
    User alice = user("alice@example.com");
    User bob = user("bob@example.com");
    repository.save(new Todo("Alice task", false, alice));
    repository.save(new Todo("Bob task", false, bob));

    List<TodoResponse> result = service.findAll(alice);

    assertEquals(List.of("Alice task"), result.stream().map(TodoResponse::title).toList());
}

@Test
void updateAndDeleteCannotCrossUserBoundary() {
    User alice = user("alice@example.com");
    User bob = user("bob@example.com");
    Todo bobTodo = repository.save(new Todo("Bob task", false, bob));

    assertThrows(ApiException.class, () ->
        service.update(alice, bobTodo.getId(), new TodoRequest("stolen", true)));
    assertThrows(ApiException.class, () ->
        service.delete(alice, bobTodo.getId()));
}
```

- [ ] **Step 2: 修改 Todo 实体和 Repository**

在 Todo 增加：

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;
```

保留无参构造器，新增 `Todo(String title, boolean done, User user)`。使用 `@JsonIgnore` 或只通过 `TodoResponse` 输出，确保 User 不被序列化。

Repository 增加：

```java
List<Todo> findByUserId(Long userId);
Optional<Todo> findByIdAndUserId(Long id, Long userId);
List<Todo> findByUserIsNull();
```

- [ ] **Step 3: 实现 Todo DTO 和用户绑定 Service**

`TodoService` 所有查询都调用带 `userId` 的 Repository 方法。新增时从参数 `User user` 设置关联；更新和删除找不到 `id + userId` 时抛出 `ApiException`（错误码 `TODO_NOT_FOUND`），不泄露另一个用户资源是否存在。

- [ ] **Step 4: 修改 Controller 使用认证主体**

Controller 方法签名使用 `@AuthenticationPrincipal UserPrincipal principal`：

```java
@GetMapping
public List<TodoResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
    return service.findAll(principal.user());
}

@PostMapping
public TodoResponse create(@AuthenticationPrincipal UserPrincipal principal,
                           @RequestBody TodoRequest request) {
    return service.create(principal.user(), request.title());
}
```

更新和删除同样传入 principal；不从请求体读取 `userId`，也不返回 User 实体。

- [ ] **Step 5: 实现系统用户初始化**

`SystemUserInitializer implements ApplicationRunner`，在事务中：

1. 查找 `system@local`，不存在时用随机不可用密码哈希创建。
2. 强制设置系统用户 `enabled=false`、`role=USER`。
3. 查询 `findByUserIsNull()`，逐条设置系统用户并保存。

初始化器不得把系统用户密码写入日志或响应。`data.sql` 保留三条演示 Todo，但注释说明 MySQL 持久化数据和 H2 演示数据都会由初始化器补充系统用户归属。

- [ ] **Step 6: 运行 Todo 隔离和迁移测试**

```bash
./mvnw -q -Dtest=TodoOwnershipTest,SystemUserInitializerTest test
```

预期：用户隔离、跨用户更新/删除拒绝、旧数据归属系统用户、系统用户不可登录测试通过。

---

### Task 6: 建立前端统一请求层和认证 API

**Files:**
- Create: `frontend/src/shared/http/request.ts`
- Create: `frontend/src/shared/http/request.test.ts`
- Create: `frontend/src/modules/auth/api/type.ts`
- Create: `frontend/src/modules/auth/api/index.ts`
- Create: `frontend/src/modules/auth/api/index.test.ts`
- Create: `frontend/src/modules/auth/constant/index.ts`

**Interfaces:**
- `request<T>(url: string, init?: RequestInit): Promise<T>`：统一解析成功 JSON、`204` 和 `ApiError`。
- `ApiError` 构造函数为 `new ApiError(status: number, code: string, message: string)`，并暴露对应三个字段。
- `register(payload): Promise<AuthUser>`。
- `login(payload): Promise<AuthUser>`。
- `logout(): Promise<void>`。
- `getCurrentUser(): Promise<AuthUser>`。
- `requestResetCode(email): Promise<void>`。
- `resetPassword(payload): Promise<void>`。

- [ ] **Step 1: 写请求层失败测试**

```ts
it('adds JSON header, same-origin credentials and parses JSON', async () => {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: async () => ({ id: 1 }),
  })
  vi.stubGlobal('fetch', fetchMock)

  await request<{ id: number }>('/api/test', {
    method: 'POST',
    body: JSON.stringify({ value: 1 }),
  })

  expect(fetchMock).toHaveBeenCalledWith('/api/test', expect.objectContaining({
    credentials: 'same-origin',
    headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
  }))
})

it('throws ApiError using backend code and message', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: false,
    status: 409,
    json: async () => ({ code: 'EMAIL_ALREADY_EXISTS', message: '邮箱不可用' }),
  }))

  await expect(request('/api/auth/register', { method: 'POST' }))
    .rejects.toMatchObject({ status: 409, code: 'EMAIL_ALREADY_EXISTS', message: '邮箱不可用' })
})
```

运行：

```bash
cd frontend && npx vitest run src/shared/http/request.test.ts
```

预期：失败，因为请求层尚未创建。

- [ ] **Step 2: 实现 CSRF 和统一请求**

请求层要求：

- 所有请求使用 `credentials: 'same-origin'`。
- `POST`、`PUT`、`PATCH`、`DELETE` 先确保 `/api/auth/csrf` 已请求过。
- 从 `document.cookie` 读取 `XSRF-TOKEN`，以 `X-XSRF-TOKEN` 请求头发送。
- `204` 直接返回 `undefined`。
- 非 2xx 尝试解析 `{ code, message }`，解析失败则使用状态码对应的通用错误。
- 不把密码、验证码或 Cookie 写入日志。

- [ ] **Step 3: 写认证 API 请求测试**

```ts
function jsonResponse(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}

function emptyResponse(): Response {
  return new Response(null, { status: 204 })
}

beforeEach(() => {
  Object.defineProperty(document, 'cookie', {
    configurable: true,
    value: 'XSRF-TOKEN=test-token',
  })
})

it('register sends email and both passwords', async () => {
  const fetchMock = vi.fn().mockResolvedValue(
    jsonResponse({ id: 1, email: 'hello@126.com', role: 'USER' }),
  )
  vi.stubGlobal('fetch', fetchMock)

  await register({ email: 'Hello@126.COM', password: 'abc12345', confirmPassword: 'abc12345' })

  expect(fetchMock).toHaveBeenCalledWith('/api/auth/register', expect.objectContaining({
    method: 'POST',
    body: JSON.stringify({ email: 'Hello@126.COM', password: 'abc12345', confirmPassword: 'abc12345' }),
  }))
})

it('login, me, logout and reset use the agreed endpoints', async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(jsonResponse({ id: 1, email: 'hello@126.com', role: 'USER' }))
    .mockResolvedValueOnce(jsonResponse({ id: 1, email: 'hello@126.com', role: 'USER' }))
    .mockResolvedValueOnce(emptyResponse())
    .mockResolvedValueOnce(emptyResponse())
  vi.stubGlobal('fetch', fetchMock)

  await login({ email: 'hello@126.com', password: 'abc12345' })
  await getCurrentUser()
  await logout()
  await resetPassword({ email: 'hello@126.com', code: '123456', newPassword: 'new12345', confirmPassword: 'new12345' })

  expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
    '/api/auth/login', '/api/auth/me', '/api/auth/logout', '/api/auth/password/reset',
  ])
})
```

- [ ] **Step 4: 实现认证类型、API 和常量**

类型定义：

```ts
export type Role = 'USER'
export interface AuthUser { id: number; email: string; role: Role }
export interface RegisterPayload { email: string; password: string; confirmPassword: string }
export interface LoginPayload { email: string; password: string }
export interface ResetPasswordPayload extends RegisterPayload { code: string }
```

常量至少包含：

```ts
export const DEMO_RESET_CODE = '123456'
export const EMAIL_UNAVAILABLE_MESSAGE = '邮箱不可用'
```

API 函数只调用请求层，不直接调用 `fetch`。成功注册不调用登录，页面由调用方负责跳转。

- [ ] **Step 5: 运行前端 API 测试和类型检查**

```bash
cd frontend
npx vitest run src/shared/http/request.test.ts src/modules/auth/api/index.test.ts
npx vue-tsc --noEmit
```

预期：请求层和认证 API 测试通过，类型检查通过。

---

### Task 7: 实现认证状态、页面、布局和路由守卫

**Files:**
- Create: `frontend/src/modules/auth/hooks/use-auth.ts`
- Create: `frontend/src/modules/auth/hooks/use-auth.test.ts`
- Create: `frontend/src/modules/auth/components/password-input.vue`
- Create: `frontend/src/modules/auth/pages/login/index.vue`
- Create: `frontend/src/modules/auth/pages/register/index.vue`
- Create: `frontend/src/modules/auth/pages/forgot-password/index.vue`
- Create: `frontend/src/layouts/admin-layout.vue`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/router/index.ts`

**Interfaces:**
- `useAuth().currentUser: Readonly<Ref<AuthUser | null>>`。
- `useAuth().loadCurrentUser(): Promise<AuthUser | null>`。
- `useAuth().setCurrentUser(user): void`。
- `useAuth().clearCurrentUser(): void`。
- `useAuth().logout(): Promise<void>`。

- [ ] **Step 1: 写认证状态测试**

```ts
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../shared/http/request'
import { getCurrentUser, logout } from '../api'
import { useAuth } from './use-auth'

vi.mock('../api', () => ({
  getCurrentUser: vi.fn(),
  logout: vi.fn(),
}))

const getCurrentUserMock = vi.mocked(getCurrentUser)

beforeEach(() => {
  getCurrentUserMock.mockReset()
  useAuth().clearCurrentUser()
})

describe('useAuth', () => {
  it('loads and stores current user', async () => {
    getCurrentUserMock.mockResolvedValue({ id: 1, email: 'a@example.com', role: 'USER' })

    const auth = useAuth()
    await expect(auth.loadCurrentUser()).resolves.toEqual({
      id: 1, email: 'a@example.com', role: 'USER',
    })
    expect(auth.currentUser.value?.email).toBe('a@example.com')
  })

  it('clears current user when me returns 401', async () => {
    getCurrentUserMock.mockRejectedValue(new ApiError(401, 'AUTH_REQUIRED', '未登录'))

    const auth = useAuth()
    await expect(auth.loadCurrentUser()).resolves.toBeNull()
    expect(auth.currentUser.value).toBeNull()
  })
})
```

- [ ] **Step 2: 实现模块级认证状态**

使用模块级 `ref`，避免每个页面得到不同的用户状态；用 `initialized` 标记避免路由守卫重复请求 `/me`。`loadCurrentUser` 对 `401` 清空状态并返回 `null`，其他错误继续抛出供错误页处理。

- [ ] **Step 3: 拆出后台布局**

把当前 `App.vue` 的 header、sider、菜单和 content 结构移动到 `admin-layout.vue`，新增：

- 当前用户邮箱显示区域。
- “退出登录”按钮。
- 退出成功后清空认证状态并跳转 `/login`。

`App.vue` 改为：

```vue
<template>
  <RouterView />
</template>
```

后台布局必须不包含登录、注册和找回密码页面。

- [ ] **Step 4: 实现登录页**

登录页包含邮箱、密码、登录按钮、注册入口和找回密码入口：

- 提交前校验邮箱非空、密码非空。
- 成功调用 `login`，写入 `currentUser`，跳转 `/list`。
- `INVALID_CREDENTIALS` 展示“邮箱或密码错误”。
- 其他错误展示后端 `message`，不展示堆栈或内部响应。

- [ ] **Step 5: 实现注册页**

注册页包含邮箱、密码、确认密码：

- 使用 `password-input.vue` 展示密码规则。
- 前端校验邮箱、两次密码、长度和至少两类组合。
- 捕获 `EMAIL_ALREADY_EXISTS` 时只显示“邮箱不可用”。
- 成功显示“注册成功，请登录”，然后跳转 `/login`。
- 不自动登录，不在页面状态中保存密码。

- [ ] **Step 6: 实现找回密码页**

页面分为请求验证码和重置密码两步：

1. 输入邮箱，点击获取验证码，调用 `/api/auth/password/reset-code`。
2. 显示“演示验证码：123456”及验证码输入框。
3. 输入验证码、新密码、确认密码，调用 reset API。
4. 成功显示“密码重置成功，请登录”，跳转 `/login`。
5. 验证码错误显示后端提示；密码规则与注册页一致。

- [ ] **Step 7: 配置路由和守卫**

将路由改成公开认证路由和受保护后台嵌套路由：

```ts
{ path: '/login', component: () => import('../modules/auth/pages/login/index.vue'), meta: { guestOnly: true } }
{ path: '/register', component: () => import('../modules/auth/pages/register/index.vue'), meta: { guestOnly: true } }
{ path: '/forgot-password', component: () => import('../modules/auth/pages/forgot-password/index.vue'), meta: { guestOnly: true } }
{
  path: '/', component: () => import('../layouts/admin-layout.vue'), meta: { requiresAuth: true },
  children: [
    { path: 'list', component: () => import('../views/list/index.vue') },
    { path: 'operation-log', component: () => import('../views/operation-log/index.vue') },
  ],
}
```

守卫规则：

- `requiresAuth` 且当前用户为空：加载 `/me`；仍为空时跳 `/login`。
- `guestOnly` 且当前用户存在：跳 `/list`。
- 根路径默认跳 `/list`。

- [ ] **Step 8: 运行前端页面相关检查**

```bash
cd frontend
npx vitest run src/modules/auth/hooks/use-auth.test.ts
npx vue-tsc --noEmit
npm run build
```

预期：认证状态测试、类型检查和生产构建通过。

---

### Task 8: 迁移现有前端 Todo API 到统一请求层并处理认证过期

**Files:**
- Modify: `frontend/src/views/list/api/index.ts`
- Modify: `frontend/src/views/list/api/index.test.ts`
- Modify: `frontend/src/views/list/index.vue`
- Modify: `frontend/src/layouts/admin-layout.vue`（若退出按钮仍需调整）

**Interfaces:**
- 既有 `listTodos`、`createTodo`、`updateTodo`、`deleteTodo` 保持导出名称和业务参数不变。
- 这些函数统一调用 `shared/http/request.ts`，不再直接调用原生 `fetch`。

- [ ] **Step 1: 更新既有 API 测试期望**

保留现有四组行为断言，但将请求断言改成包含：

```ts
expect.objectContaining({
  credentials: 'same-origin',
  headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
})
```

并新增：

```ts
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
```

- [ ] **Step 2: 替换 Todo API 的直接 fetch**

`listTodos`、`createTodo`、`updateTodo`、`deleteTodo` 全部从 `../../../shared/http/request` 导入 `request`。保留原有 API URL 和请求体，不添加 `userId`。

- [ ] **Step 3: 在列表页处理 Session 过期**

`load`、保存、更新、删除捕获 `ApiError`：

- `status === 401`：清空当前用户并跳转 `/login`。
- 其他错误：使用 ant-design-vue message 显示 `error.message`。
- 保持现有本地搜索、表格分页和权限配置行为不变。

- [ ] **Step 4: 运行前端回归检查**

```bash
cd frontend
npx vitest run src/views/list/api/index.test.ts src/views/list/hooks src/shared src/modules/auth
npx vue-tsc --noEmit
npm run build
```

预期：原有 11 个测试及新增认证测试均通过，构建成功。

---

### Task 9: 完成端到端安全验收、文档和最终验证

**Files:**
- Modify: `README.md`
- Create: `src/test/java/com/example/demo/auth/AuthFlowIntegrationTest.java`

- [ ] **Step 1: 写后端认证流集成测试**

用 MockMvc 验收完整顺序：

1. 注册 `alice@example.com`。
2. 重复注册同邮箱，断言 HTTP `409`、内部码 `EMAIL_ALREADY_EXISTS`、消息“邮箱不可用”。
3. 登录 Alice，保存 Session Cookie。
4. 用 Alice 创建 Todo，断言返回不含 `user` 或 `userId`。
5. 注册并登录 Bob，断言 Bob 的列表为空。
6. Bob 无法用 Alice 的 Todo ID 更新或删除。
7. Alice 可看到和修改自己的 Todo。
8. Alice 退出后访问 `/api/todos` 返回 `401`。
9. 使用演示验证码 `123456` 重置密码后，新密码可以登录旧密码不能登录。

- [ ] **Step 2: 验证旧数据迁移和系统用户不可登录**

使用测试数据创建无 `user_id` 的 Todo，启动 `SystemUserInitializer`，断言：

```java
assertEquals("system@local", migratedTodo.getUser().getEmail());
assertFalse(systemUser.isEnabled());
```

调用登录接口使用系统用户凭据，断言返回 `401`。

- [ ] **Step 3: 更新 README 运行说明**

在 [README.md](../../../README.md) 增加：

- 注册、登录和找回密码页面路径。
- 演示验证码为 `123456`，明确只适合开发演示。
- 密码规则：至少 8 位，三类组合满足至少两类。
- Todo 按当前登录用户隔离。
- 第一版不发送真实邮件，后续接入邮箱服务的位置。
- 需要使用 MySQL 或测试 H2 配置启动后端。

不要把数据库密码、测试用户密码或 Session Cookie 写入 README。

- [ ] **Step 4: 执行完整验证**

后端：

```bash
./mvnw -q test
./mvnw -q package -DskipTests
```

前端：

```bash
cd frontend
npx vitest run
npx vue-tsc --noEmit
npm run build
```

手工启动：

```bash
# 终端一
./mvnw spring-boot:run

# 终端二
cd frontend && npm run dev
```

在浏览器按以下顺序验收：

- 直接打开 `/#/list` 会跳到登录页。
- 注册新邮箱成功后跳转登录。
- 同邮箱再次注册显示“邮箱不可用”。
- 登录后显示后台布局和当前邮箱。
- 新增 Todo 后退出，再注册另一个账号，第二个账号看不到第一个账号的 Todo。
- 使用 `123456` 找回密码后可以用新密码登录。
- 刷新受保护页面仍保持登录；退出后刷新不能重新访问 Todo。

## Plan Self-Review

### Spec coverage

- 邮箱小写规范化：Task 1、Task 2、Task 3、Task 6。
- 重复邮箱内部码和“邮箱不可用”文案：Task 2、Task 4、Task 7、Task 9。
- 密码 8 位和两类组合：Task 1、Task 2、Task 7。
- Argon2：Task 2、Task 3、Task 4、Task 9。
- Session、HttpOnly Cookie 和 CSRF：Task 3、Task 4、Task 6。
- 注册、登录、退出、当前用户和重置密码 API：Task 4、Task 6、Task 7。
- Todo 用户隔离：Task 5、Task 8、Task 9。
- `system@local` 和历史数据迁移：Task 5、Task 9。
- 前端 pages/views 边界和后台布局：Task 7。
- 第一版普通角色和未来扩展边界：Task 2、Task 3、Task 7。
- 前后端单测、集成测试、构建和手工验收：每个任务的测试步骤以及 Task 9。

### Placeholder scan

计划中的路径、类名、方法名、错误码、请求体、测试命令和验收步骤均已明确；没有使用未确定项或实现占位。

### Type consistency

- `AuthUserResponse` 与前端 `AuthUser` 均只包含 `id`、`email`、`role`。
- 注册和重置均使用 `email/password/confirmPassword`；重置额外使用 `code`。
- `TodoService` 接收 `User` 和 Todo ID/请求，不接收 `userId` 字符串参数给 Controller。
- `request<T>` 统一承载认证 API 和 Todo API 的 JSON/204/错误返回。
- 路由使用 `requiresAuth` 和 `guestOnly` 元信息，与 `useAuth` 的当前用户状态一致。
