package com.stk.auth_service.controller;

import com.jayway.jsonpath.JsonPath;
import com.stk.auth_service.model.User;
import com.stk.auth_service.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureMockMvc
@Transactional
class AuthFlowTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository repository;
    @Autowired PasswordEncoder passwords;
    private static final String PASSWORD = "TestPassword123!";

    private User user() {
        User user = new User();
        user.setUsername("user" + UUID.randomUUID().toString().replace("-", ""));
        user.setEmail(user.getUsername() + "@example.com");
        user.setPassword(passwords.encode(PASSWORD));
        return repository.saveAndFlush(user);
    }
    private String credentials(User user, String password) {
        return "{\"username\":\"" + user.getUsername() + "\",\"password\":\"" + password + "\"}";
    }
    @Test
    void registersANormalizedUserWithAHashAndNeverExposesPassword() throws Exception {
        String username = "User" + UUID.randomUUID().toString().replace("-", "");
        String body = "{\"username\":\"" + username + "\",\"email\":\"" + username
                + "@example.com\",\"password\":\"" + PASSWORD + "\",\"role\":\"ADMIN\"}";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value(username.toLowerCase()))
                .andExpect(jsonPath("$.password").doesNotExist());
        User saved = repository.findByUsernameIgnoreCase(username).orElseThrow();
        assertTrue(passwords.matches(PASSWORD, saved.getPassword()));
        assertNotEquals(PASSWORD, saved.getPassword());
        assertEquals("USER", saved.getRole()); assertTrue(saved.isActive()); assertFalse(saved.isDeleted());
    }
    @Test
    void logsInAndUsesTheIssuedTokenToReadTheCurrentUser() throws Exception {
        User user = user();
        String response = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials(user, PASSWORD))).andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.accessToken");
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(user.getId().toString()));
    }
    @Test
    void rejectsIncorrectCredentialsAndUnavailableAccounts() throws Exception {
        User user = user();
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials(user, "wrongpassword")))
                .andExpect(status().isUnauthorized());
        user.setActive(false); repository.saveAndFlush(user);
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials(user, PASSWORD)))
                .andExpect(status().isUnauthorized());
        user.setActive(true); user.setDeleted(true); repository.saveAndFlush(user);
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials(user, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void rejectsDuplicatesAndInvalidRegistration() throws Exception {
        User user = user();
        String body = "{\"username\":\"" + user.getUsername().toUpperCase() + "\",\"email\":\"other@example.com\",\"password\":\"" + PASSWORD + "\"}";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ab\",\"email\":\"invalid\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void requiresAValidTokenForMe() throws Exception {
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/me").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void rejectsUnconfiguredBrowserOrigins() throws Exception {
        // This test uses an explicit disallowed origin; live preflight also checks the configured portal port.
        mvc.perform(options("/auth/login").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
