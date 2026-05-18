package com.example.WorkHub.config;

import com.example.WorkHub.tenant.MultiTenantConnectionProviderImpl;
import com.example.WorkHub.tenant.TenantIdentifierResolver;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class HibernateConfig {

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            DataSource dataSource,
            MultiTenantConnectionProviderImpl provider,
            TenantIdentifierResolver resolver
    ) {

        Map<String, Object> props = new HashMap<>();

        props.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, provider);
        props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);

        return builder
                .dataSource(dataSource)
                .packages("com.example.WorkHub.model")
                .properties(props)
                .build();
    }
}
