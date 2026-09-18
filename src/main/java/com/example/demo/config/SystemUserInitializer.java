package com.example.demo.config;

import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.User;
import com.example.demo.auth.repository.UserRepository;
import com.example.demo.entity.Todo;
import com.example.demo.repository.TodoRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 启动时的数据迁移：把历史无归属的待办绑定到不可登录的系统用户。
 *
 * 系统用户 {@code system@local}：
 * - 角色 USER，状态禁用（{@code enabled=false}），只作为历史数据的归属者；
 * - 若不存在则用一个随机不可预测的密码哈希创建，密码不写日志、不可用、不可登录。
 *
 * 迁移在一个事务里完成，保证「创建系统用户 + 回填待办」要么全成功要么全回滚。
 */
@Component
public class SystemUserInitializer implements ApplicationRunner {

    static final String SYSTEM_EMAIL = "system@local";

    private static final Logger log = LoggerFactory.getLogger(SystemUserInitializer.class);

    private final UserRepository userRepository;

    private final TodoRepository todoRepository;

    private final PasswordEncoder passwordEncoder;

    public SystemUserInitializer(UserRepository userRepository, TodoRepository todoRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.todoRepository = todoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User system = userRepository.findByEmail(SYSTEM_EMAIL).orElseGet(() -> {
            // 随机且不可预测的密码：无法被猜到，也从不记录到日志或响应。
            User created = User.create(SYSTEM_EMAIL,
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    Role.USER, false);
            log.info("已创建系统用户 {}（禁用，用于历史数据归属）", SYSTEM_EMAIL);
            return userRepository.save(created);
        });

        // 即使系统用户已存在，也强制保持「禁用 + USER」，避免被误启用后登录。
        system.setEnabled(false);
        system.setRole(Role.USER);
        userRepository.save(system);

        List<Todo> orphans = todoRepository.findByUserIsNull();
        if (!orphans.isEmpty()) {
            orphans.forEach(todo -> todo.setUser(system));
            todoRepository.saveAll(orphans);
            log.info("已将 {} 条无归属待办迁移到系统用户 {}", orphans.size(), SYSTEM_EMAIL);
        }
    }
}
