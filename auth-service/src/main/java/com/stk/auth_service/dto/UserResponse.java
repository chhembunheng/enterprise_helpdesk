package com.stk.auth_service.dto;

public record UserResponse(
    java.util.UUID id,
    String username,
    String email
) {
}
