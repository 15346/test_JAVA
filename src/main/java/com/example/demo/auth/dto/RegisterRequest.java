package com.example.demo.auth.dto;

public record RegisterRequest(String email, String password, String confirmPassword) {
}
