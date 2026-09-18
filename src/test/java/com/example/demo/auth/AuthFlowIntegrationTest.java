package com.example.demo.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.config.SystemUserInitializer;
import com.example.demo.entity.Todo;
import com.example.demo.repository.TodoRepository;
import com.example.demo.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 认证与 Todo 归属的端到端验收流。
 *
 * MockMvc 不会模拟 Servlet 容器下发 JSESSIONID Cookie，因此测试直接复用登录请求
 * 保存的 {@link MockHttpSession}，等价验证 Session 中的 Spring Security 上下文。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    private static final String ALICE_EMAIL = "alice@example.com";

    private static final String ALICE_PASSWORD = "Alice123!";

    private static final String BOB_EMAIL = "bob@example.com";

    private static final String BOB_PASSWORD = "Bob12345!";

    private static final String ALICE_NEW_PASSWORD = "AliceNew123!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private SystemUserInitializer systemUserInitializer;

    @BeforeEach
    void resetDatabase() {
        // 其他 Spring 测试可能复用同一个 H2 上下文；清理后重新运行初始化器，
        // 保证系统用户存在且每次验收流都从干净数据开始。
        todoRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        systemUserInitializer.run(null);
    }

    @AfterEach
    void cleanDatabase() {
        // 不把本验收流的用户和待办留给其他复用同一 Spring 上下文的测试。
        todoRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void completesRegistrationIsolationLogoutAndPasswordResetFlow() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(ALICE_EMAIL, ALICE_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(ALICE_EMAIL));

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(ALICE_EMAIL, ALICE_PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("邮箱不可用"));

        MvcResult aliceLogin = mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(ALICE_EMAIL, ALICE_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ALICE_EMAIL))
                .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", org.hamcrest.Matchers.notNullValue()))
                .andReturn();
        MockHttpSession aliceSession = sessionFrom(aliceLogin);

        mvc.perform(post("/api/todos")
                        .session(aliceSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Alice task\",\"done\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Alice task"))
                .andExpect(jsonPath("$.done").value(false))
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist());

        Todo aliceTodo = todoRepository.findAll().stream().findFirst().orElseThrow();
        Long aliceTodoId = aliceTodo.getId();

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(BOB_EMAIL, BOB_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(BOB_EMAIL));

        MvcResult bobLogin = mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(BOB_EMAIL, BOB_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(BOB_EMAIL))
                .andReturn();
        MockHttpSession bobSession = sessionFrom(bobLogin);

        mvc.perform(get("/api/todos").session(bobSession))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mvc.perform(put("/api/todos/{id}", aliceTodoId)
                        .session(bobSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bob attack\",\"done\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"));

        mvc.perform(delete("/api/todos/{id}", aliceTodoId)
                        .session(bobSession)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"));

        mvc.perform(get("/api/todos").session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(aliceTodoId.intValue()))
                .andExpect(jsonPath("$[0].title").value("Alice task"));

        mvc.perform(put("/api/todos/{id}", aliceTodoId)
                        .session(aliceSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Alice updated\",\"done\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceTodoId.intValue()))
                .andExpect(jsonPath("$.title").value("Alice updated"))
                .andExpect(jsonPath("$.done").value(true));

        mvc.perform(post("/api/auth/logout")
                        .session(aliceSession)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/todos").session(aliceSession))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"code\":\"123456\","
                                + "\"newPassword\":\"AliceNew123!\","
                                + "\"confirmPassword\":\"AliceNew123!\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(ALICE_EMAIL, ALICE_NEW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ALICE_EMAIL));

        mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(ALICE_EMAIL, ALICE_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("system@local", "anything")))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession sessionFrom(MvcResult result) {
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session, "登录必须创建 HttpSession");
        return session;
    }

    private String registerJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password
                + "\",\"confirmPassword\":\"" + password + "\"}";
    }

    private String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
