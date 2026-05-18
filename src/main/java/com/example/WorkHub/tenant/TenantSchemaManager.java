package com.example.WorkHub.tenant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "spring.tenancy.mode", havingValue = "schema")
public class TenantSchemaManager {

    private final JdbcTemplate jdbcTemplate;
    private final TenantSchemaResolver tenantSchemaResolver;

    public TenantSchemaManager(JdbcTemplate jdbcTemplate, TenantSchemaResolver tenantSchemaResolver) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantSchemaResolver = tenantSchemaResolver;
    }

    public void ensureTenantSchema(UUID tenantId) {
        jdbcTemplate.execute((java.sql.Connection connection) -> {
            String schema = tenantSchemaResolver.resolveSchema(connection, tenantId);
            String publicSchema = tenantSchemaResolver.resolvePublicSchema(connection);
            if (publicSchema.equalsIgnoreCase(schema)) {
                return null;
            }

            jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS %s.project (
                        id UUID PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        created_by VARCHAR(255) NOT NULL,
                        tenant_id UUID NOT NULL,
                        CONSTRAINT fk_%s_project_tenant
                            FOREIGN KEY (tenant_id) REFERENCES %s.tenant(id)
                    )
                    """.formatted(schema, schema, publicSchema));
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS %s.task (
                        id UUID PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        status VARCHAR(2) NOT NULL,
                        project_id UUID NOT NULL,
                        tenant_id UUID NOT NULL,
                        version BIGINT,
                        CONSTRAINT fk_%s_task_project
                            FOREIGN KEY (project_id) REFERENCES %s.project(id),
                        CONSTRAINT fk_%s_task_tenant
                            FOREIGN KEY (tenant_id) REFERENCES %s.tenant(id)
                    )
                    """.formatted(schema, schema, schema, schema, publicSchema));
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS %s.job (
                        id UUID PRIMARY KEY,
                        status VARCHAR(255) NOT NULL,
                        project_id UUID NOT NULL,
                        tenant_id UUID NOT NULL,
                        CONSTRAINT fk_%s_job_project
                            FOREIGN KEY (project_id) REFERENCES %s.project(id),
                        CONSTRAINT fk_%s_job_tenant
                            FOREIGN KEY (tenant_id) REFERENCES %s.tenant(id)
                    )
                    """.formatted(schema, schema, schema, schema, publicSchema));
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS %s.processed_message (
                        message_key VARCHAR(255) PRIMARY KEY,
                        processed_at TIMESTAMP WITH TIME ZONE NOT NULL
                    )
                    """.formatted(schema));
            return null;
        });
    }

    public String resolveSchemaName(UUID tenantId) {
        return jdbcTemplate.execute((java.sql.Connection connection) ->
                tenantSchemaResolver.resolveSchema(connection, tenantId));
    }
}
