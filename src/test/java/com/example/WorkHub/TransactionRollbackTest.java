package com.example.WorkHub;

import com.example.WorkHub.jwt.TenantAuthenticationToken;
import com.example.WorkHub.model.Tenant;
import com.example.WorkHub.repository.ProjectRepository;
import com.example.WorkHub.repository.TenantRepository;
import com.example.WorkHub.service.ProjectService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
public class TransactionRollbackTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setName("RollbackTestTenant");
        tenant.setPlan("Basic");
        tenant = tenantRepository.save(tenant);
        
        TenantAuthenticationToken auth = new TenantAuthenticationToken(
                "user@example.com", null, tenant.getId(), List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.execute("DELETE FROM task");
        jdbcTemplate.execute("DELETE FROM project");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM tenant");
    }

    @Test
    void createProject_rollsBack_whenExceptionThrown() {
        long countBefore = projectRepository.count();

        assertThrows(RuntimeException.class, () -> {
            projectService.createProjectAndFail("Should Not Exist");
        });

        long countAfter = projectRepository.count();
        assertEquals(countBefore, countAfter, "Project count should remain unchanged after rollback");
    }
}
