package com.example.WorkHub.tenant;

import com.example.WorkHub.jwt.TenantAuthenticationToken;
import com.example.WorkHub.model.Project;
import com.example.WorkHub.model.Tenant;
import com.example.WorkHub.repository.ProjectRepository;
import com.example.WorkHub.repository.TenantRepository;
import com.example.WorkHub.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class TenantIsolationIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private com.example.WorkHub.service.ProjectService projectService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Tenant tenantA;
    private Tenant tenantB;

    @BeforeEach
    void setUp() {
        // Create Tenant A
        tenantA = new Tenant();
        tenantA.setName("Tenant A");
        tenantA.setPlan("Basic");
        tenantRepository.save(tenantA);

        // Create Tenant B
        tenantB = new Tenant();
        tenantB.setName("Tenant B");
        tenantB.setPlan("Premium");
        tenantRepository.save(tenantB);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.execute("DELETE FROM job");
        jdbcTemplate.execute("DELETE FROM task");
        jdbcTemplate.execute("DELETE FROM project");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM tenant");
    }

    private void authenticateAsTenant(UUID tenantId) {
        TenantAuthenticationToken auth = new TenantAuthenticationToken(
                "user@example.com", null, tenantId, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void testAutoPopulation() {
        // Authenticate as Tenant A
        authenticateAsTenant(tenantA.getId());

        // Create a project
        Project project = new Project();
        project.setName("Project A");
        project.setCreatedBy("userA");
        project = projectRepository.save(project);
        
        // Fetch to see what was saved in DB
        Optional<Project> retrieved = projectRepository.findById(project.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getTenantId()).isEqualTo(tenantA.getId().toString());
    }

    @Test
    void testCrossTenantIsolation() {
        // 1. Authenticate as Tenant A and create a project
        authenticateAsTenant(tenantA.getId());
        Project projectA = new Project();
        projectA.setName("Top Secret Project A");
        projectA.setCreatedBy("userA");
        projectA = projectRepository.save(projectA);
        
        UUID projectAUuid = projectA.getId();

        // 2. Authenticate as Tenant B
        authenticateAsTenant(tenantB.getId());

        // 3. Attempt to fetch Tenant A's project by UUID
        Optional<Project> retrievedByB = projectRepository.findById(projectAUuid);

        // 4. Expectation: The discriminator filter should block it
        assertThat(retrievedByB).isEmpty();

        // Attempt to fetch all projects
        List<Project> allProjectsForB = projectRepository.findAll();
        assertThat(allProjectsForB).isEmpty();
    }

    @Test
    void testElaborateTenantError() {
        // 1. Authenticate as Tenant A and create a project
        authenticateAsTenant(tenantA.getId());
        Project projectA = new Project();
        projectA.setName("Elaborate Test Project");
        projectA.setCreatedBy("userA");
        projectA = projectRepository.save(projectA);
        UUID projectAUuid = projectA.getId();

        // 2. Authenticate as Tenant B
        authenticateAsTenant(tenantB.getId());

        // 3. Attempt to fetch via Service, expecting ResourceNotFoundException
        org.junit.jupiter.api.Assertions.assertThrows(
            com.example.WorkHub.exception.ResourceNotFoundException.class,
            () -> projectService.getProjectById(projectAUuid)
        );
    }
}
