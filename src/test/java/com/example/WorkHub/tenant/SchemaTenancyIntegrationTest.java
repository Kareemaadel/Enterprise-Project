package com.example.WorkHub.tenant;

import com.example.WorkHub.jwt.TenantAuthenticationToken;
import com.example.WorkHub.model.Project;
import com.example.WorkHub.model.Tenant;
import com.example.WorkHub.repository.ProjectRepository;
import com.example.WorkHub.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.tenancy.mode=schema")
class SchemaTenancyIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantSchemaManager tenantSchemaManager;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Tenant tenantA;
    private Tenant tenantB;

    @BeforeEach
    void setUp() {
        tenantA = new Tenant();
        tenantA.setName("Schema Tenant A");
        tenantA.setPlan("BASIC");
        tenantA = tenantRepository.save(tenantA);
        tenantSchemaManager.ensureTenantSchema(tenantA.getId());

        tenantB = new Tenant();
        tenantB.setName("Schema Tenant B");
        tenantB.setPlan("PRO");
        tenantB = tenantRepository.save(tenantB);
        tenantSchemaManager.ensureTenantSchema(tenantB.getId());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        dropSchema(tenantA);
        dropSchema(tenantB);
        jdbcTemplate.execute("DELETE FROM PUBLIC.job");
        jdbcTemplate.execute("DELETE FROM PUBLIC.task");
        jdbcTemplate.execute("DELETE FROM PUBLIC.project");
        jdbcTemplate.execute("DELETE FROM public.users");
        jdbcTemplate.execute("DELETE FROM public.tenant");
    }

    @Test
    void schemaModeStoresProjectsInTenantSpecificSchema() {
        authenticateAsTenant(tenantA.getId());

        Project project = new Project();
        project.setName("Tenant A Project");
        project.setCreatedBy("tenant-a@example.com");
        Project saved = projectRepository.save(project);

        String schemaA = tenantSchemaManager.resolveSchemaName(tenantA.getId());
        String schemaB = tenantSchemaManager.resolveSchemaName(tenantB.getId());

        Integer countInSchemaA = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + schemaA + ".project WHERE id = ?",
                Integer.class,
                saved.getId());
        Integer countInSchemaB = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + schemaB + ".project WHERE id = ?",
                Integer.class,
                saved.getId());

        assertThat(countInSchemaA).isEqualTo(1);
        assertThat(countInSchemaB).isEqualTo(0);
        assertThat(tenantRepository.findById(tenantA.getId())).isPresent();
    }

    @Test
    void schemaModePreventsCrossTenantProjectReads() {
        authenticateAsTenant(tenantA.getId());

        Project project = new Project();
        project.setName("Secret Project");
        project.setCreatedBy("tenant-a@example.com");
        project = projectRepository.save(project);

        authenticateAsTenant(tenantB.getId());

        Optional<Project> retrievedByTenantB = projectRepository.findById(project.getId());
        assertThat(retrievedByTenantB).isEmpty();
        assertThat(projectRepository.findAll()).isEmpty();
    }

    private void authenticateAsTenant(UUID tenantId) {
        TenantAuthenticationToken auth = new TenantAuthenticationToken(
                "user@example.com", null, tenantId, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void dropSchema(Tenant tenant) {
        if (tenant == null || tenant.getId() == null) {
            return;
        }

        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + tenantSchemaManager.resolveSchemaName(tenant.getId()) + " CASCADE");
    }
}
