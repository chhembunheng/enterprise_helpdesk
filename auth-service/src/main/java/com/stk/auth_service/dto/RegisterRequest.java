package com.stk.auth_service.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[a-zA-Z0-9_.-]+") String username,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 8, max = 64) String password
) {
    public RegisterRequest {
        username = username == null ? null : username.trim();
        email = email == null ? null : email.trim();
    }
}
