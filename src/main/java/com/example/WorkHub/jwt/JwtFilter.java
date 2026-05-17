package com.example.WorkHub.jwt;

import com.example.WorkHub.jwt.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);
    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        logger.info("Request URI: {}", request.getRequestURI());
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            logger.info("token: {}", token);
            if (!jwtUtil.validateJwtToken(token)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid bearer token");
                return;
            }

            String email = jwtUtil.getEmailFromToken(token);
            logger.info("email: {}", email);
            logger.info("in jwt filter");

            String tenantIdClaim = jwtUtil.getTenantIdFromToken(token);
            java.util.UUID tenantId = null;
            if (tenantIdClaim != null && !tenantIdClaim.isBlank()) {
                try {
                    tenantId = java.util.UUID.fromString(tenantIdClaim);
                } catch (IllegalArgumentException e) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid tenant id");
                    return;
                }
            }

           String tenantRoleClaim = jwtUtil.getTenantRoleFromToken(token);
           if(tenantRoleClaim == null || tenantRoleClaim.isBlank() || (!tenantRoleClaim.equals("TENANT_ADMIN") && !tenantRoleClaim.equals("TENANT_USER"))){
               response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid tenant role");
               return;
           }

            var authToken = new TenantAuthenticationToken(
                    email, null, tenantId, List.of(new SimpleGrantedAuthority(tenantRoleClaim))
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
            logger.info("Auth set: authenticated={}, authorities={}",
                authToken.isAuthenticated(), authToken.getAuthorities());
        }

        chain.doFilter(request, response);
    }
}
