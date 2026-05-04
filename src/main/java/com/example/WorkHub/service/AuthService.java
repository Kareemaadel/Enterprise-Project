package com.example.WorkHub.service;

import com.example.WorkHub.dto.AuthMeResponse;
import com.example.WorkHub.jwt.JwtUtil;
import com.example.WorkHub.model.Tenant;
import com.example.WorkHub.model.User;
import com.example.WorkHub.repository.TenantRepository;
import com.example.WorkHub.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.WorkHub.jwt.TenantAuthenticationToken;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public String register(String email, String password, String tenantIdValue, String role) {

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        UUID tenantId;
        try {
            tenantId = UUID.fromString(tenantIdValue);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tenantId format");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password)); // hash
        user.setTenant(tenant);
        user.setRole(role);

        userRepository.save(user);

        String tenantIdClaim = user.getTenant() != null ? user.getTenant().getId().toString() : null;
        return jwtUtil.generateToken(email, tenantIdClaim,role);
    }

    public String login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String tenantId = user.getTenant() != null ? user.getTenant().getId().toString() : null;
        String role = user.getRole();
        return jwtUtil.generateToken(email, tenantId, role);
    }

    public AuthMeResponse me(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated user");
        }

        UUID tenantId = null;
        String tenantRole = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof TenantAuthenticationToken tenantAuth) {
            tenantId = tenantAuth.getTenantId();
            tenantRole = tenantAuth.getAuthorities()
                    .stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse(null);
        }

        return new AuthMeResponse(
                email,
                tenantId,
                tenantRole
                );
    }
}
