package com.stk.auth_service.controller;

import com.stk.auth_service.dto.*;
import com.stk.auth_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) { return service.login(request); }
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) { return service.me(jwt.getSubject()); }
}
