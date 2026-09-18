package com.example.demo.auth.controller;

import com.example.demo.auth.dto.AuthUserResponse;
import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.RegisterRequest;
import com.example.demo.auth.dto.ResetCodeRequest;
import com.example.demo.auth.dto.ResetPasswordRequest;
import com.example.demo.auth.entity.User;
import com.example.demo.auth.security.UserPrincipal;
import com.example.demo.auth.service.AuthService;
import com.example.demo.auth.service.EmailNormalizer;
import com.example.demo.auth.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。退出（{@code POST /api/auth/logout}）不在这里映射，由 Spring Security 的
 * {@code LogoutFilter} 处理。
 *
 * 所有响应只暴露 {@link AuthUserResponse}（id/email/role），不含密码、哈希或 Session ID。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    private final PasswordResetService passwordResetService;

    private final AuthenticationManager authenticationManager;

    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthService authService, PasswordResetService passwordResetService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    /** 注册成功返回 201，但不自动登录（不建立 Session）。 */
    @PostMapping("/register")
    public ResponseEntity<AuthUserResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * 手动 JSON 登录：认证成功后把 SecurityContext 写入 Session，
     * 后续请求即可凭 HttpOnly 的 Session Cookie 保持登录态。
     */
    @PostMapping("/login")
    public AuthUserResponse login(@RequestBody LoginRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        EmailNormalizer.normalize(request.email()), request.password()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // 会话固定防护：仅当请求已持有 Session 时才轮换其 ID（与 Spring Security 的
        // SessionFixationProtectionStrategy 一致——无既有会话时不存在固定风险，
        // 且此时 changeSessionId() 会抛异常），随后再持久化认证上下文。
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return toResponse(((UserPrincipal) authentication.getPrincipal()).user());
    }

    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return toResponse(principal.user());
    }

    /**
     * 接收 {@link CsrfToken} 参数，迫使令牌解析，从而把 XSRF-TOKEN Cookie 写给前端。
     */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        return ResponseEntity.noContent().build();
    }

    /** 请求重置验证码：第一版不发邮件，对任何邮箱都返回 204。 */
    @PostMapping("/password/reset-code")
    public ResponseEntity<Void> requestResetCode(@RequestBody ResetCodeRequest request) {
        passwordResetService.requestCode(request.email());
        return ResponseEntity.noContent().build();
    }

    /** 用固定验证码重置密码，成功返回 204，且不创建 Session。 */
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    private AuthUserResponse toResponse(User user) {
        return new AuthUserResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
