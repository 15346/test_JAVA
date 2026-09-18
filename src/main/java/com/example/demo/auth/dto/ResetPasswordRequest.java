package com.example.demo.auth.dto;

public record ResetPasswordRequest(String email, String code, String newPassword,
                                   String confirmPassword) {
}
