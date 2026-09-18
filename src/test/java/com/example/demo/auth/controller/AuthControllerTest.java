package com.example.demo.auth.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.User;
import com.example.demo.auth.repository.UserRepository;
import com.example.demo.auth.security.UserPrincipal;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 认证接口的集成测试：注册、JSON 登录、当前用户、CSRF、退出和固定验证码重置密码。
 *
 * 测试用户一律通过 {@link PasswordEncoder} 生成哈希后落库，不写明文。
 *
 * 登录下发 JSESSIONID Cookie 属于 Servlet 容器职责，MockMvc 不模拟，由
 * {@link AuthSessionCookieTest} 用真实端口覆盖。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String USER_EMAIL = "user@example.com";
    private static final String EXISTING_EMAIL = "existing@example.com";
    private static final String RAW_PASSWORD = "abc12345";

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository repository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUsers() {
        // 同一 H2 内存库在测试 JVM 内被复用，先清空避免邮箱在测试方法之间泄漏。
        repository.deleteAll();
        repository.save(User.create(USER_EMAIL,
                passwordEncoder.encode(RAW_PASSWORD), Role.USER, true));
        repository.save(User.create(EXISTING_EMAIL,
                passwordEncoder.encode(RAW_PASSWORD), Role.USER, true));
    }

    private UserPrincipal principal(String email) {
        return new UserPrincipal(repository.findByEmail(email).orElseThrow());
    }

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
    void successfulRegistrationReturnsCreatedAndDoesNotLogIn() throws Exception {
        MvcResult register = mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" Fresh@Example.COM \",\"password\":\"abc12345\",\"confirmPassword\":\"abc12345\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.email").value("fresh@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();

        // 注册不自动登录：该请求不得建立携带认证上下文的 Session。
        HttpSession session = register.getRequest().getSession(false);
        assertTrue(session == null || session.getAttribute("SPRING_SECURITY_CONTEXT") == null,
                "注册接口不得建立登录态");
    }

    @Test
    void loginCreatesSessionAndMeReturnsUser() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"abc12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.role").value("USER"))
                // 登录把认证上下文持久化进 HttpSession，真实容器据此下发 JSESSIONID Cookie；
                // MockMvc 不模拟容器的 Set-Cookie，因此这里断言 Session 侧的等价效果。
                .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", notNullValue()))
                .andReturn();

        // 同一次登录建立的 Session 可以继续访问 /me。
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(USER_EMAIL));
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
    void meReturnsAuthenticatedUser() throws Exception {
        mvc.perform(get("/api/auth/me").with(user(principal(USER_EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void csrfEndpointReturnsNoContent() throws Exception {
        // 只断言 204：XSRF-TOKEN Cookie 由 CookieCsrfTokenRepository 写出，
        // 但同一测试类里一旦用过 .with(csrf())，Spring Security 的测试用
        // TestCsrfTokenRepository 会接管仓库（改为写 Session），Cookie 便不可断言。
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent());
    }

    @Test
    void logoutReturnsNoContent() throws Exception {
        mvc.perform(post("/api/auth/logout")
                        .with(user(principal(USER_EMAIL)))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void resetAcceptsOnlyDemoCodeAndNewPasswordPolicy() throws Exception {
        User before = repository.findByEmail(USER_EMAIL).orElseThrow();

        mvc.perform(post("/api/auth/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"code\":\"123456\",\"newPassword\":\"new12345\",\"confirmPassword\":\"new12345\"}"))
                .andExpect(status().isNoContent());

        User after = repository.findByEmail(USER_EMAIL).orElseThrow();
        assertNotEquals(before.getPasswordHash(), after.getPasswordHash());
        assertTrue(passwordEncoder.matches("new12345", after.getPasswordHash()),
                "新密码必须以 Argon2 哈希保存");
    }

    @Test
    void resetValidatesPasswordShapeBeforeEmailOrCodeLookup() throws Exception {
        // 弱密码必须在邮箱/验证码查询之前被拒绝：即便邮箱不存在、验证码也无效，
        // 也应返回 PASSWORD_POLICY_INVALID 而非 RESET_CODE_INVALID，避免枚举账号。
        mvc.perform(post("/api/auth/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"does-not-exist@example.com\",\"code\":\"123456\",\"newPassword\":\"weak\",\"confirmPassword\":\"weak\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_POLICY_INVALID"));
    }

    @Test
    void resetWithWrongCodeIsRejected() throws Exception {
        mvc.perform(post("/api/auth/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"code\":\"000000\",\"newPassword\":\"new12345\",\"confirmPassword\":\"new12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESET_CODE_INVALID"))
                .andExpect(jsonPath("$.message").value("验证码错误"));
    }

    @Test
    void resetCodeRequestDoesNotRevealWhetherEmailExists() throws Exception {
        mvc.perform(post("/api/auth/password/reset-code")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void malformedJsonReturnsBadRequestInsteadOfServerError() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
