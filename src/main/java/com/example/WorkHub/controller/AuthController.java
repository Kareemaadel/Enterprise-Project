package com.example.WorkHub.controller;

import com.example.WorkHub.dto.AuthMeResponse;
import com.example.WorkHub.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public String register(@RequestBody Map<String, String> body) {
        return authService.register(body.get("email"), body.get("password"), body.get("tenantId"),body.get("tenantRole"));
    }

    @GetMapping("/login")
    public String login(@RequestBody Map<String,String> body) {
        return authService.login(body.get("email"), body.get("password"));
    }

    @GetMapping("/me")
    public AuthMeResponse me(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return authService.me(email);
    }
}
