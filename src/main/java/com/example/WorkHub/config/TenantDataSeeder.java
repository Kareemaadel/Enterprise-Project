package com.example.WorkHub.config;

import com.example.WorkHub.model.Tenant;
import com.example.WorkHub.repository.TenantRepository;
import com.example.WorkHub.tenant.TenantSchemaManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Profile("!test")
public class TenantDataSeeder implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final TenantSchemaManager tenantSchemaManager;

    public TenantDataSeeder(TenantRepository tenantRepository,
                            ObjectProvider<TenantSchemaManager> tenantSchemaManagerProvider) {
        this.tenantRepository = tenantRepository;
        this.tenantSchemaManager = tenantSchemaManagerProvider.getIfAvailable();
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<TenantSeed> seeds = List.of(
                new TenantSeed("Oscorp Industires", "BASIC"),
                new TenantSeed("FawryPay", "PRO"),
                new TenantSeed("Initech", "ENTERPRISE"));

        Set<String> existingNames = tenantRepository.findAll()
                .stream()
                .map(Tenant::getName)
                .collect(Collectors.toSet());

        List<Tenant> missingTenants = seeds.stream()
                .filter(seed -> !existingNames.contains(seed.name()))
                .map(seed -> {
                    Tenant tenant = new Tenant();
                    tenant.setName(seed.name());
                    tenant.setPlan(seed.plan());
                    return tenant;
                })
                .toList();

        if (!missingTenants.isEmpty()) {
            tenantRepository.saveAll(missingTenants);
        }

        if (tenantSchemaManager != null) {
            tenantRepository.findAll()
                    .forEach(tenant -> tenantSchemaManager.ensureTenantSchema(tenant.getId()));
        }

        // Dump a tenant ID into a file for automated testing scripts
        // this is only for testing purposes dont worry about it :D
        // this whole class is for testing purposes anyways.
        try {
            List<Tenant> allTenants = tenantRepository.findAll();
            if (!allTenants.isEmpty()) {
                String firstTenantId = allTenants.get(0).getId().toString();
                java.nio.file.Files.writeString(
                        java.nio.file.Path.of(".tenant_ids.txt"),
                        firstTenantId,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to write .tenant_ids.txt for testing script");
        }
    }

    private record TenantSeed(String name, String plan) {
    }
}
