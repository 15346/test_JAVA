package com.example.demo.auth.security;

import com.example.demo.config.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * 安全配置：密码编码器、认证管理器、Session、CSRF 与 URL 授权。
 *
 * - 密码使用 Argon2（BouncyCastle 提供实现）。
 * - 认证管理器基于 DaoAuthenticationProvider，用户来自数据库。
 * - CSRF 令牌放在可被前端读取的 Cookie 中（HttpOnly=false）。
 * - 未登录访问受保护接口返回 JSON 401，而不是默认的 403 跳转。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public AuthenticationManager authenticationManager(CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * 手动 JSON 登录需要把认证上下文写入 Session，Task 4 会注入该 Bean。
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper)
            throws Exception {
        // 匿名访问受保护资源时返回 JSON 401；Spring Security 默认是 403，
        // 与前端约定的 { code: "AUTH_REQUIRED", message: "未登录" } 不符。
        AuthenticationEntryPoint authenticationEntryPoint = (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), new ApiError("AUTH_REQUIRED", "未登录"));
        };

        http
                // spa()：Cookie 保存 CSRF 令牌（HttpOnly=false，前端可读）并启用 SPA 专用请求处理器，
                // 使前端可以直接把 XSRF-TOKEN Cookie 值放进 X-XSRF-TOKEN 请求头（默认的 XOR 处理器不接受）。
                .csrf(csrf -> csrf.spa())
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
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        // SPA 期望退出接口返回 204；默认的 SimpleUrlLogoutSuccessHandler
                        // 会 302 跳到 /login?logout，前端 fetch 会把重定向当成异常。
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT))
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"));

        return http.build();
    }
}
