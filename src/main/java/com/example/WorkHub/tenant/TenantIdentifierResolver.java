package com.example.WorkHub.tenant;

import com.example.WorkHub.jwt.TenantAuthenticationToken;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<java.util.UUID> {

    private static final java.util.UUID SYSTEM_TENANT = java.util.UUID.fromString(
            "00000000-0000-0000-0000-000000000000");

    @Override
    public java.util.UUID resolveCurrentTenantIdentifier() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof TenantAuthenticationToken tenantAuth) {
            java.util.UUID tenantId = tenantAuth.getTenantId();
            if (tenantId != null) {
                System.out.println("RESOLVER: Found tenant " + tenantId);
                return tenantId;
            }
        }
        System.out.println("RESOLVER: Fallback to SYSTEM");
        // Fallback for public/unauthenticated endpoints like login
        return SYSTEM_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        // MUST return true to ensure Hibernate does not reuse sessions
        // across different tenants, preventing silent data leaks.
        return true;
    }
}
