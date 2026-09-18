package com.example.demo.auth.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Test
    void findsUserByNormalizedEmail() {
        User user = User.create("hello@126.com", "hash", Role.USER, true);
        repository.save(user);

        assertTrue(repository.findByEmail("hello@126.com").isPresent());
        assertTrue(repository.existsByEmail("hello@126.com"));
    }

    @Test
    void persistsAllFieldsOnSavedUser() {
        User saved = repository.save(
                User.create("hello@126.com", "argon2-hash", Role.USER, true));

        User reloaded = repository.findByEmail("hello@126.com").orElseThrow();
        assertEquals(saved.getId(), reloaded.getId());
        assertEquals("hello@126.com", reloaded.getEmail());
        assertEquals("argon2-hash", reloaded.getPasswordHash());
        assertEquals(Role.USER, reloaded.getRole());
        assertTrue(reloaded.isEnabled());
    }

    @Test
    void reportsMissingEmailAsAbsent() {
        assertFalse(repository.findByEmail("nobody@126.com").isPresent());
        assertFalse(repository.existsByEmail("nobody@126.com"));
    }
}
