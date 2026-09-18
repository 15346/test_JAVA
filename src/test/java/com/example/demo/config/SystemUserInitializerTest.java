package com.example.demo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.User;
import com.example.demo.auth.repository.UserRepository;
import com.example.demo.auth.security.CustomUserDetailsService;
import com.example.demo.entity.Todo;
import com.example.demo.repository.TodoRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * 系统用户初始化：历史无归属待办被迁移到禁用的 {@code system@local}，
 * 且该账号无法登录。
 */
@DataJpaTest
@Import({SystemUserInitializer.class, SystemUserInitializerTest.EncoderConfig.class})
@ActiveProfiles("test")
class SystemUserInitializerTest {

    private static final String SYSTEM_EMAIL = "system@local";

    @Autowired
    private SystemUserInitializer initializer;

    @Autowired
    private TodoRepository repository;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * {@link SystemUserInitializer} 实现了 {@code ApplicationRunner}，切片上下文启动时
     * 就会执行一次并把系统用户提交进共享的 H2 库。为保证每个测试独立，先清空两张表
     * （清理在测试事务内，方法结束会回滚）。
     *
     * 用 {@code deleteAllInBatch}：它立即执行批量 DELETE，而普通的 {@code deleteAll}
     * 只是把删除排进 Hibernate 的动作队列，而 IDENTITY 主键的新增会先于排队的删除执行，
     * 导致「清空 + 重新插入同一邮箱」撞上唯一约束。
     */
    @BeforeEach
    void clearData() {
        repository.deleteAllInBatch();
        users.deleteAllInBatch();
    }

    @Test
    void assignsUnownedTodosToDisabledSystemUser() {
        Todo legacy = repository.save(new Todo("legacy task", false, null));
        assertEquals(1, repository.findByUserIsNull().size());

        initializer.run(null);

        User system = users.findByEmail(SYSTEM_EMAIL).orElseThrow();
        assertFalse(system.isEnabled(), "系统用户必须禁用");
        assertEquals(Role.USER, system.getRole());

        assertTrue(repository.findByUserIsNull().isEmpty(), "无归属待办必须全部迁移");
        Todo migrated = repository.findById(legacy.getId()).orElseThrow();
        assertEquals(SYSTEM_EMAIL, migrated.getUser().getEmail());
    }

    @Test
    void keepsForceDisabledWhenSystemUserAlreadyExists() {
        users.save(User.create(SYSTEM_EMAIL, passwordEncoder.encode("enabled-secret"),
                Role.USER, true));

        initializer.run(null);

        List<User> systems = users.findAll().stream()
                .filter(user -> SYSTEM_EMAIL.equals(user.getEmail()))
                .toList();
        assertEquals(1, systems.size(), "初始化不得重复创建系统用户");
        assertFalse(systems.get(0).isEnabled(), "已存在的系统用户也必须被强制禁用");
    }

    @Test
    void systemUserCannotAuthenticate() {
        initializer.run(null);

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(new CustomUserDetailsService(users));
        provider.setPasswordEncoder(passwordEncoder);
        AuthenticationManager manager = new ProviderManager(provider);

        assertThrows(DisabledException.class, () -> manager.authenticate(
                new UsernamePasswordAuthenticationToken(SYSTEM_EMAIL, "anything")));
    }

    @TestConfiguration
    static class EncoderConfig {

        @Bean
        PasswordEncoder passwordEncoder() {
            return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        }
    }
}
