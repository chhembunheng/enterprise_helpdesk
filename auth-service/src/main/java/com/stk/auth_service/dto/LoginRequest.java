package com.stk.auth_service.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
    @NotBlank @Size(max = 50) String username,
    @NotBlank @Size(max = 64) String password
) {}
