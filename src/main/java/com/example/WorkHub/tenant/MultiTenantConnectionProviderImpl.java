package com.example.WorkHub.tenant;
import com.example.WorkHub.tenant.strategy.TenancyStrategy;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class MultiTenantConnectionProviderImpl
        implements MultiTenantConnectionProvider<UUID> {

    private final DataSource dataSource;
    private final TenancyStrategy strategy;

    public MultiTenantConnectionProviderImpl(
            DataSource dataSource,
            TenancyStrategy strategy
    ) {
        this.dataSource = dataSource;
        this.strategy = strategy;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public Connection getConnection(UUID tenantIdentifier) throws SQLException {

        Connection connection = getAnyConnection();

        strategy.applySchema(connection, tenantIdentifier);

        return connection;
    }

    @Override
    public void releaseConnection(UUID tenantIdentifier, Connection connection)
            throws SQLException {

        try {
            strategy.reset(connection);
        } finally {
            connection.close();
        }
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}
