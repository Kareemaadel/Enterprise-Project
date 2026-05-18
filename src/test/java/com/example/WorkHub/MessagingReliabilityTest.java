package com.example.WorkHub;

import com.example.WorkHub.dto.ReportRequestEvent;
import com.example.WorkHub.jwt.TenantAuthenticationToken;
import com.example.WorkHub.messaging.ReportProducer;
import com.example.WorkHub.model.Job;
import com.example.WorkHub.model.Project;
import com.example.WorkHub.model.Tenant;
import com.example.WorkHub.repository.*;
import com.example.WorkHub.service.ReportService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {"spring.tenancy.mode=discriminator"})
@Testcontainers(disabledWithoutDocker = true)
public class MessagingReliabilityTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportProducer reportProducer;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ProcessedMessageRepository processedMessageRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Tenant testTenant;
    private Project testProject;

    @BeforeEach
    void setUp() {
        testTenant = new Tenant();
        testTenant.setName("MessagingTestTenant");
        testTenant.setPlan("PRO");
        testTenant = tenantRepository.save(testTenant);

        // Set authentication BEFORE saving the project
        TenantAuthenticationToken auth = new TenantAuthenticationToken(
                "user@example.com", null, testTenant.getId(), List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        testProject = new Project();
        testProject.setName("Messaging Project");
        testProject.setCreatedBy("user@test.com");
        testProject = projectRepository.save(testProject);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.execute("DELETE FROM processed_message");
        jdbcTemplate.execute("DELETE FROM job");
        jdbcTemplate.execute("DELETE FROM task");
        jdbcTemplate.execute("DELETE FROM project");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM tenant");
    }

    @Test
    void publishReport_consumerProcesses_jobCompletedInDb() {
        Job job = reportService.enqueueReportGeneration(testProject.getId());

        TenantAuthenticationToken auth = new TenantAuthenticationToken(
                "user@example.com", null, testTenant.getId(), List.of());

        Awaitility.await()
                .pollInSameThread()
                .atMost(20, TimeUnit.SECONDS)
                .until(() -> {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    try {
                        return jobRepository.findById(job.getId())
                                .map(j -> "COMPLETED".equals(j.getStatus()))
                                .orElse(false);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });

        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            Job finalJob = jobRepository.findById(job.getId()).orElseThrow();
            assertEquals("COMPLETED", finalJob.getStatus());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void duplicateMessage_idempotencyKey_processedOnlyOnce() {
        Job job = reportService.enqueueReportGeneration(testProject.getId());

        // Manually publish the same event a second time
        ReportRequestEvent duplicateEvent = new ReportRequestEvent(
                job.getId(),
                testProject.getId(),
                testTenant.getId(),
                "user@test.com"
        );
        reportProducer.sendReportRequest(duplicateEvent);

        TenantAuthenticationToken auth = new TenantAuthenticationToken(
            "user@example.com", null, testTenant.getId(), List.of());

        Awaitility.await()
            .pollInSameThread()
                .atMost(20, TimeUnit.SECONDS)
            .until(() -> {
                SecurityContextHolder.getContext().setAuthentication(auth);
                try {
                return jobRepository.findById(job.getId())
                    .map(j -> "COMPLETED".equals(j.getStatus()))
                    .orElse(false);
                } finally {
                SecurityContextHolder.clearContext();
                }
            });

        assertTrue(processedMessageRepository.existsById(job.getId().toString()));
        
        long count = processedMessageRepository.findAll().stream()
                .filter(m -> m.getMessageKey().equals(job.getId().toString()))
                .count();
        assertEquals(1, count, "Message should be processed only once despite duplicates");
    }
}
