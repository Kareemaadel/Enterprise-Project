package com.example.WorkHub.tenant.strategy;

import com.example.WorkHub.tenant.TenantSchemaResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        name = "spring.tenancy.mode",
        havingValue = "discriminator",
        matchIfMissing = true
)
public class DiscriminatorTenancyStrategy implements TenancyStrategy {

    private final TenantSchemaResolver tenantSchemaResolver;

    public DiscriminatorTenancyStrategy(TenantSchemaResolver tenantSchemaResolver) {
        this.tenantSchemaResolver = tenantSchemaResolver;
    }

    @Override
    public void applySchema(Connection connection, UUID tenantId) throws SQLException {
        connection.setSchema(tenantSchemaResolver.resolvePublicSchema(connection));
    }

    @Override
    public void reset(Connection connection) throws SQLException {
        connection.setSchema(tenantSchemaResolver.resolvePublicSchema(connection));
    }
}
