package com.stk.auth_service.service;

import com.stk.auth_service.dto.*;
import com.stk.auth_service.mapper.UserMapper;
import com.stk.auth_service.model.User;
import com.stk.auth_service.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwords;
    private final JwtEncoder tokens;
    private final String issuer;
    private final String audience;
    private final long ttl;
    private final String dummyHash;

    public AuthService(UserRepository repository, UserMapper mapper, PasswordEncoder passwords, JwtEncoder tokens,
                       @Value("${helpdesk.jwt.issuer}") String issuer,
                       @Value("${helpdesk.jwt.audience}") String audience,
                       @Value("${helpdesk.jwt.ttl-seconds}") long ttl) {
        if (ttl < 60 || ttl > 86400) throw new IllegalArgumentException("JWT TTL must be 60 to 86400 seconds");
        this.repository = repository; this.mapper = mapper; this.passwords = passwords;
        this.tokens = tokens; this.issuer = issuer; this.audience = audience; this.ttl = ttl;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password exceeds 72 UTF-8 bytes");
        if (repository.existsByUsernameIgnoreCase(username) || repository.existsByEmailIgnoreCase(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already registered");
        User user = new User();
        user.setUsername(username); user.setEmail(email);
        user.setPassword(passwords.encode(request.password()));
        user.setRole("USER"); user.setActive(true); user.setDeleted(false);
        try { return mapper.toResponse(repository.saveAndFlush(user)); }
        catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already registered");
        }
    }

    public LoginResponse login(LoginRequest request) {
        User user = repository.findByUsernameIgnoreCase(request.username().trim()).orElse(null);
        boolean valid = request.password().getBytes(StandardCharsets.UTF_8).length <= 72
                && passwords.matches(request.password(), user == null ? dummyHash : user.getPassword());
        if (!valid || user == null || !user.isActive() || user.isDeleted())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience))
                .subject(user.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(ttl)).build();
        String token = tokens.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", ttl, mapper.toResponse(user));
    }

    public UserResponse me(String id) {
        User user = repository.findById(UUID.fromString(id)).filter(u -> u.isActive() && !u.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account unavailable"));
        return mapper.toResponse(user);
    }
}
