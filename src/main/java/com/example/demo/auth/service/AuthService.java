package com.example.demo.auth.service;

import com.example.demo.auth.dto.AuthUserResponse;
import com.example.demo.auth.dto.RegisterRequest;
import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.User;
import com.example.demo.auth.repository.UserRepository;
import com.example.demo.config.ApiException;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Pattern EMAIL =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository repository;

    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthUserResponse register(RegisterRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());
        if (normalizedEmail == null || !EMAIL.matcher(normalizedEmail).matches()) {
            throw new ApiException(400, "INVALID_EMAIL", "邮箱格式不正确");
        }
        if (request.password() == null
                || !request.password().equals(request.confirmPassword())) {
            throw new ApiException(400, "PASSWORD_CONFIRMATION_MISMATCH", "两次输入的密码不一致");
        }
        if (!PasswordPolicy.isValid(request.password())) {
            throw new ApiException(400, "PASSWORD_POLICY_INVALID", "密码不符合安全要求");
        }
        if (repository.existsByEmail(normalizedEmail)) {
            throw new ApiException(409, "EMAIL_ALREADY_EXISTS", "邮箱不可用");
        }

        User user = User.create(normalizedEmail,
                passwordEncoder.encode(request.password()), Role.USER, true);
        User saved = repository.save(user);
        return new AuthUserResponse(saved.getId(), saved.getEmail(), saved.getRole());
    }

    public Optional<User> findByEmail(String rawEmail) {
        return repository.findByEmail(EmailNormalizer.normalize(rawEmail));
    }
}
