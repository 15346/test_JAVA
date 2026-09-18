package com.example.demo.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.User;
import com.example.demo.auth.repository.UserRepository;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * 真实 Tomcat + 真实 HTTP 的会话 Cookie 验证。
 *
 * 登录成功必须下发 JSESSIONID；这是 Session 登录态的载体，但属于 Servlet 容器的职责，
 * MockMvc 不模拟（spring-test 的 MockHttpServletResponse 不因创建 Session 而写 Cookie），
 * 因此在真实端口上断言。
 *
 * 顺带覆盖前端契约：先 GET /api/auth/csrf 拿 XSRF-TOKEN，再把原始令牌放进
 * X-XSRF-TOKEN 请求头（csrf().spa() 的明文头策略）。
 *
 * 注意：本类刻意不使用 Spring Security 测试的 {@code .with(csrf())}——
 * 它会替换底层 CsrfTokenRepository，导致 Cookie 断言失真。因此本类不启用 MockMvc。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthSessionCookieTest {

    private static final String USER_EMAIL = "user@example.com";
    private static final String RAW_PASSWORD = "abc12345";

    @Value("${local.server.port}")
    int port;

    @Autowired
    UserRepository repository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUser() {
        repository.deleteAll();
        repository.save(User.create(USER_EMAIL,
                passwordEncoder.encode(RAW_PASSWORD), Role.USER, true));
    }

    @Test
    void loginIssuesJsessionIdCookieAndMeAcceptsIt() throws Exception {
        CookieManager cookieJar = new CookieManager();
        HttpClient client = HttpClient.newBuilder().cookieHandler(cookieJar).build();
        String base = "http://localhost:" + port;

        HttpResponse<String> csrf = client.send(
                HttpRequest.newBuilder(URI.create(base + "/api/auth/csrf")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(204, csrf.statusCode());
        String xsrf = cookieValue(cookieJar, "XSRF-TOKEN");
        assertNotNull(xsrf, "csrf 接口应下发 XSRF-TOKEN Cookie");

        HttpResponse<String> login = client.send(
                HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .header("X-XSRF-TOKEN", xsrf)
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"email\":\"user@example.com\",\"password\":\"abc12345\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, login.statusCode());
        assertNotNull(cookieValue(cookieJar, "JSESSIONID"),
                "登录成功必须下发 JSESSIONID Cookie");

        // 带上下发的 Session Cookie 访问 /me，登录态成立。
        HttpResponse<String> me = client.send(
                HttpRequest.newBuilder(URI.create(base + "/api/auth/me")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, me.statusCode());
        assertTrue(me.body().contains(USER_EMAIL), "/me 应返回当前登录用户");
    }

    private static String cookieValue(CookieManager cookieJar, String name) {
        return cookieJar.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals(name))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
