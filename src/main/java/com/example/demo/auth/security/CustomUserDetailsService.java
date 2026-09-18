package com.example.demo.auth.security;

import com.example.demo.auth.entity.User;
import com.example.demo.auth.repository.UserRepository;
import com.example.demo.auth.service.EmailNormalizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 按规范化邮箱从数据库加载用户，供 DaoAuthenticationProvider 使用。
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repository;

    public CustomUserDetailsService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserPrincipal loadUserByUsername(String rawEmail) throws UsernameNotFoundException {
        String normalizedEmail = EmailNormalizer.normalize(rawEmail);
        User user = repository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        return new UserPrincipal(user);
    }
}
