package com.example.WorkHub.tenant.strategy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

public interface TenancyStrategy {
    void applySchema(Connection connection, UUID tenantId) throws SQLException;

    void reset(Connection connection) throws SQLException;
}
