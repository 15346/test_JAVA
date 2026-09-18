package com.example.demo.auth.service;

import com.example.demo.auth.dto.ResetPasswordRequest;
import com.example.demo.auth.entity.User;
import com.example.demo.auth.repository.UserRepository;
import com.example.demo.config.ApiException;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 找回密码：第一版不发邮件，使用固定验证码完成重置闭环。
 *
 * 固定值集中在此处，后续接入真实邮件服务时只需替换 {@link #DEMO_CODE} 的校验。
 */
@Service
public class PasswordResetService {

    /** 演示用固定验证码；第一版不接邮件，仅接受该值。 */
    private static final String DEMO_CODE = "123456";

    private final UserRepository repository;

    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 请求验证码。第一版不发邮件，对不存在的邮箱也按成功处理，
     * 避免响应差异暴露账号是否存在。
     */
    public void requestCode(String rawEmail) {
        // 有意不查询用户、不发邮件：响应与邮箱是否存在无关。
    }

    /**
     * 校验邮箱与固定验证码，成功返回对应用户。
     *
     * 邮箱不存在或验证码错误统一抛出同一错误，避免枚举账号。
     */
    public User verify(String rawEmail, String code) {
        Optional<User> found = repository.findByEmail(EmailNormalizer.normalize(rawEmail));
        if (found.isEmpty() || !DEMO_CODE.equals(code)) {
            throw new ApiException(400, "RESET_CODE_INVALID", "验证码错误");
        }
        return found.get();
    }

    /**
     * 校验固定验证码、重复密码和密码规则后，用 Argon2 重新编码并保存新密码。
     * 不创建 Session，也不记录明文密码。
     */
    public void resetPassword(ResetPasswordRequest request) {
        if (request.newPassword() == null
                || !request.newPassword().equals(request.confirmPassword())) {
            throw new ApiException(400, "PASSWORD_CONFIRMATION_MISMATCH", "两次输入的密码不一致");
        }
        if (!PasswordPolicy.isValid(request.newPassword())) {
            throw new ApiException(400, "PASSWORD_POLICY_INVALID", "密码不符合安全要求");
        }

        User user = verify(request.email(), request.code());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        repository.save(user);
    }
}
