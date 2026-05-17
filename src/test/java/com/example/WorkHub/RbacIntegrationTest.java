package com.example.WorkHub;

import com.example.WorkHub.jwt.JwtUtil;
import com.example.WorkHub.model.Tenant;
import com.example.WorkHub.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID testTenantId;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setPlan("BASIC");
        tenant = tenantRepository.save(tenant);
        testTenantId = tenant.getId();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM task");
        jdbcTemplate.execute("DELETE FROM project");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM tenant");
    }

    private String buildJwt(String tenantId, String role) {
        return jwtUtil.generateToken("user@test.com", tenantId, role);
    }

    @Test
    void missingToken_returns401() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRole_returns403() throws Exception {
        String token = buildJwt(testTenantId.toString(), "TENANT_USER");

        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\",\"createdBy\":\"user@test.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRole_canCreateProject_returns201() throws Exception {
        String token = buildJwt(testTenantId.toString(), "TENANT_ADMIN");

        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Admin Project\",\"createdBy\":\"admin@test.com\"}"))
                .andExpect(status().isCreated());
    }
}
