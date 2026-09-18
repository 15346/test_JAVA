package com.example.demo.auth.dto;

import com.example.demo.auth.entity.Role;

public record AuthUserResponse(Long id, String email, Role role) {
}
