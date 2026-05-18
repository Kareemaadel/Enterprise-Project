package com.example.WorkHub.tenant;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class TenantSchemaResolver {

    public static final String PUBLIC_SCHEMA = "public";
    private static final UUID SYSTEM_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public String resolveSchema(UUID tenantId) {
        if (tenantId == null || SYSTEM_TENANT.equals(tenantId)) {
            return PUBLIC_SCHEMA;
        }

        return "tenant_" + tenantId.toString().replace('-', '_');
    }

    public String resolveSchema(Connection connection, UUID tenantId) throws SQLException {
        return normalizeSchemaName(resolveSchema(tenantId), connection);
    }

    public String resolvePublicSchema(Connection connection) throws SQLException {
        if (isH2(connection)) {
            return "PUBLIC";
        }

        return PUBLIC_SCHEMA;
    }

    public String normalizeSchemaName(String schema, Connection connection) throws SQLException {
        if (isH2(connection)) {
            return schema.toUpperCase();
        }

        return schema;
    }

    private boolean isH2(Connection connection) throws SQLException {
        String databaseProduct = connection.getMetaData().getDatabaseProductName();
        return databaseProduct != null && databaseProduct.toLowerCase().contains("h2");
    }
}
