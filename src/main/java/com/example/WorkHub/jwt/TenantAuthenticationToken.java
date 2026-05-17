package com.example.WorkHub.jwt;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

public class TenantAuthenticationToken extends AbstractAuthenticationToken {
    
    private final Object principal;
    private final Object credentials;
    private final UUID tenantId;

    public TenantAuthenticationToken(Object principal, Object credentials, UUID tenantId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        this.tenantId = tenantId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public UUID getTenantId() {
        return tenantId;
    }
}
