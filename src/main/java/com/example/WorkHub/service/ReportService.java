package com.example.WorkHub.service;

import com.example.WorkHub.dto.ReportRequestEvent;
import com.example.WorkHub.jwt.TenantAuthenticationToken;
import com.example.WorkHub.messaging.ReportProducer;
import com.example.WorkHub.model.Job;
import com.example.WorkHub.model.Project;
import com.example.WorkHub.model.Tenant;
import com.example.WorkHub.repository.JobRepository;
import com.example.WorkHub.repository.TenantRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final MeterRegistry meterRegistry;
    private final JobRepository jobRepository;
    private final TenantRepository tenantRepository;
    private final ProjectService projectService;
    private final ReportProducer reportProducer;

    public ReportService(MeterRegistry meterRegistry,
                         JobRepository jobRepository,
                         TenantRepository tenantRepository,
                         ProjectService projectService,
                         ReportProducer reportProducer) {
        this.meterRegistry = meterRegistry;
        this.jobRepository = jobRepository;
        this.tenantRepository = tenantRepository;
        this.projectService = projectService;
        this.reportProducer = reportProducer;
    }

    /**
     * Enqueues a report generation job via Kafka.
     * 1. Creates a Job entity in PENDING state
     * 2. Publishes a ReportRequestEvent to the Kafka topic
     * 3. The consumer will process the event asynchronously and update the Job status
     *
     * @param projectId the project to generate a report for
     * @return the created Job entity with PENDING status
     */
    @Transactional
    public Job enqueueReportGeneration(UUID projectId) {
        // Resolve project (also validates it exists)
        Project project = projectService.getProjectById(projectId);

        // Extract tenant context from the authenticated user
        TenantAuthenticationToken auth = (TenantAuthenticationToken)
                SecurityContextHolder.getContext().getAuthentication();
        UUID tenantId = auth.getTenantId();
        String requestedBy = (String) auth.getPrincipal();

        // Create the Job entity in PENDING state
        Job job = new Job();
        job.setStatus("PENDING");
        job.setProject(project);
        job = jobRepository.save(job);

        // Publish event to Kafka
        ReportRequestEvent event = new ReportRequestEvent(
                job.getId(),
                projectId,
                tenantId,
                requestedBy
        );
        reportProducer.sendReportRequest(event);

        logger.info("Report generation enqueued: jobId={}, projectId={}, tenantId={}",
                job.getId(), projectId, tenantId);

        // Increment the custom metric
        Counter.builder("workhub.reports.generated")
                .tag("tenantId", tenantId.toString())
                .description("Number of reports generated")
                .register(meterRegistry)
                .increment();

        return job;
    }

    /**
     * Retrieves all jobs/reports for a given project.
     */
    @Transactional(readOnly = true)
    public List<Job> getReportsByProjectId(UUID projectId) {
        // Validate project exists
        projectService.getProjectById(projectId);
        return jobRepository.findByProjectId(projectId);
    }
}
