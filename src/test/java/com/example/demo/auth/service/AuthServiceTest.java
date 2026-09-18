package com.example.demo.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.auth.dto.AuthUserResponse;
import com.example.demo.auth.dto.RegisterRequest;
import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.User;
import com.example.demo.auth.repository.UserRepository;
import com.example.demo.config.ApiException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private UserRepository repository;
    private PasswordEncoder encoder;
    private AuthService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(UserRepository.class);
        encoder = Mockito.mock(PasswordEncoder.class);
        service = new AuthService(repository, encoder);
    }

    @Test
    void registerNormalizesEmailAndStoresOnlyEncodedPassword() {
        when(repository.existsByEmail("hello@126.com")).thenReturn(false);
        when(encoder.encode("abc12345")).thenReturn("argon2-hash");
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthUserResponse result = service.register(
                new RegisterRequest(" Hello@126.COM ", "abc12345", "abc12345"));

        verify(encoder).encode("abc12345");
        verify(repository).save(argThat(user ->
                user.getEmail().equals("hello@126.com")
                        && user.getPasswordHash().equals("argon2-hash")
                        && user.getRole() == Role.USER
                        && user.isEnabled()));
        assertEquals("hello@126.com", result.email());
        assertEquals(Role.USER, result.role());
    }

    @Test
    void duplicateEmailThrowsEmailAlreadyExists() {
        when(repository.existsByEmail("hello@126.com")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () ->
                service.register(new RegisterRequest(
                        "Hello@126.com", "abc12345", "abc12345")));

        assertEquals("EMAIL_ALREADY_EXISTS", exception.getCode());
        assertEquals(409, exception.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void invalidEmailIsRejectedBeforeAnyLookup() {
        ApiException exception = assertThrows(ApiException.class, () ->
                service.register(new RegisterRequest(
                        "not-an-email", "abc12345", "abc12345")));

        assertEquals("INVALID_EMAIL", exception.getCode());
        verify(repository, never()).existsByEmail(any());
        verify(repository, never()).save(any());
    }

    @Test
    void mismatchedConfirmationIsRejected() {
        ApiException exception = assertThrows(ApiException.class, () ->
                service.register(new RegisterRequest(
                        "hello@126.com", "abc12345", "abc123456")));

        assertEquals("PASSWORD_CONFIRMATION_MISMATCH", exception.getCode());
        verify(repository, never()).existsByEmail(any());
        verify(repository, never()).save(any());
    }

    @Test
    void weakPasswordIsRejectedBeforeAnyLookup() {
        ApiException exception = assertThrows(ApiException.class, () ->
                service.register(new RegisterRequest(
                        "hello@126.com", "abcdefgh", "abcdefgh")));

        assertEquals("PASSWORD_POLICY_INVALID", exception.getCode());
        verify(repository, never()).existsByEmail(any());
        verify(repository, never()).save(any());
    }

    @Test
    void findByEmailNormalizesInput() {
        User user = User.create("hello@126.com", "hash", Role.USER, true);
        when(repository.findByEmail("hello@126.com")).thenReturn(Optional.of(user));

        assertTrue(service.findByEmail("  Hello@126.COM ").isPresent());
        verify(repository).findByEmail("hello@126.com");
    }
}
