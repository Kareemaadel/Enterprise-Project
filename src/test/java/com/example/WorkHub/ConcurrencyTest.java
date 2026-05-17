package com.example.WorkHub;

import com.example.WorkHub.jwt.TenantAuthenticationToken;
import com.example.WorkHub.model.Project;
import com.example.WorkHub.model.Task;
import com.example.WorkHub.model.TaskStatus;
import com.example.WorkHub.model.Tenant;
import com.example.WorkHub.repository.ProjectRepository;
import com.example.WorkHub.repository.TaskRepository;
import com.example.WorkHub.repository.TenantRepository;
import com.example.WorkHub.service.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class ConcurrencyTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Task testTask;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        testTenant = new Tenant();
        testTenant.setName("ConcurrencyTestTenant");
        testTenant.setPlan("PRO");
        testTenant = tenantRepository.save(testTenant);

        // Set authentication BEFORE saving the project or task
        TenantAuthenticationToken auth = new TenantAuthenticationToken(
                "user@example.com", null, testTenant.getId(), List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Project project = new Project();
        project.setName("Concurrency Project");
        project.setCreatedBy("user@test.com");
        project = projectRepository.save(project);

        testTask = new Task();
        testTask.setTitle("Concurrent Task");
        testTask.setStatus(TaskStatus.P);
        testTask.setProject(project);
        testTask = taskRepository.save(testTask);
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
    void concurrentUpdates_optimisticLocking_handleConflictsCorrectly() throws InterruptedException {
        int threadCount = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger conflicts = new AtomicInteger(0);
        AtomicInteger otherErrors = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    // Each thread needs its own security context if security is checked inside the service
                    TenantAuthenticationToken auth = new TenantAuthenticationToken(
                            "user@example.com", null, testTenant.getId(), List.of());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    
                    taskService.updateTaskStatus(testTask.getId(), TaskStatus.IP);
                } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                    conflicts.incrementAndGet();
                } catch (Exception e) {
                    otherErrors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        pool.shutdown();

        // One thread should succeed, at least 4 should fail
        assertEquals(threadCount - 1, conflicts.get() + otherErrors.get(), 
                "Exactly 4 threads should have failed due to concurrent modification");
        
        Task finalTask = taskRepository.findById(testTask.getId()).orElseThrow();
        assertEquals(TaskStatus.IP, finalTask.getStatus(), "Task status should be IP after one successful update");
    }
}
