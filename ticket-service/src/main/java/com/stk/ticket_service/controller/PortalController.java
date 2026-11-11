package com.stk.ticket_service.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
public class PortalController {
    private final String authUrl;
    public PortalController(@Value("${helpdesk.auth-url}") String authUrl) { this.authUrl = authUrl; }
    @GetMapping("/client-config")
    public Map<String, String> config() { return Map.of("authUrl", authUrl); }
}
