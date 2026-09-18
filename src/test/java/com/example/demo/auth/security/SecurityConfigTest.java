package com.example.demo.auth.security;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    MockMvc mvc;

    @Test
    void todoRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/todos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authRegisterEndpointIsPublic() throws Exception {
        // 注册接口必须公开：安全层不能把它挡下来（控制器在 Task 4 才实现，
        // 所以这里断言"既不是 401 也不是 403"，而不是断言 2xx 成功）。
        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"password\":\"abc12345\",\"confirmPassword\":\"abc12345\"}"))
                .andExpect(status().is(not(HttpStatus.UNAUTHORIZED.value())))
                .andExpect(status().is(not(HttpStatus.FORBIDDEN.value())));
    }
}
