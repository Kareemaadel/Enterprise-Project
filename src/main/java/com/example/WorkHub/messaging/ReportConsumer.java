package com.example.WorkHub.messaging;

import com.example.WorkHub.config.KafkaConfig;
import com.example.WorkHub.dto.ReportRequestEvent;
import com.example.WorkHub.jwt.TenantAuthenticationToken;
import com.example.WorkHub.model.Job;
import com.example.WorkHub.model.ProcessedMessage;
import com.example.WorkHub.repository.JobRepository;
import com.example.WorkHub.repository.ProcessedMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Kafka consumer that processes report generation requests.
 * 
 * Reliability strategy: idempotency via a processed-message table.
 * Before processing, the consumer checks if a message with the same key
 * has already been handled. If so, it skips the message (safe retry).
 * 
 * The entire consume-check-process-mark flow is wrapped in a single
 * @Transactional boundary so that if any step fails, the DB state
 * is rolled back and the message will be retried by Kafka.
 */
@Component
public class ReportConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ReportConsumer.class);

    private final JobRepository jobRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final ObjectMapper objectMapper;

    public ReportConsumer(JobRepository jobRepository,
                          ProcessedMessageRepository processedMessageRepository,
                          ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.processedMessageRepository = processedMessageRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Listens for report-request messages from Kafka.
     * On receipt, checks idempotency, simulates report generation,
     * and marks the Job as COMPLETED.
     *
     * @param message the raw JSON message from Kafka
     */
    @KafkaListener(topics = KafkaConfig.REPORT_TOPIC, groupId = "workhub-report-group")
    public void consume(String message) {
        ReportRequestEvent event;
        try {
            event = objectMapper.readValue(message, ReportRequestEvent.class);
        } catch (Exception e) {
            logger.error("Failed to deserialize report request message: {}", e.getMessage());
            return; // skip poison pill messages
        }

        TenantAuthenticationToken auth = new TenantAuthenticationToken(
                event.requestedBy(), null, event.tenantId(), List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            handleEvent(event);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Transactional
    protected void handleEvent(ReportRequestEvent event) {
        String messageKey = event.jobId().toString();

        // Idempotency check: skip if already processed
        if (processedMessageRepository.existsById(messageKey)) {
            logger.info("Message with key {} already processed, skipping (idempotent)", messageKey);
            return;
        }

        logger.info("Processing report request for job {} (project={}, tenant={})",
                event.jobId(), event.projectId(), event.tenantId());

        // Look up the Job entity
        Job job = jobRepository.findById(event.jobId()).orElse(null);
        if (job == null) {
            logger.warn("Job {} not found, skipping", event.jobId());
            return;
        }

        // Simulate report generation work
        try {
            Thread.sleep(2000); // simulate 2 seconds of processing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Report generation interrupted for job {}", event.jobId());
            return;
        }

        // Mark job as completed
        job.setStatus("COMPLETED");
        jobRepository.save(job);

        // Record the message as processed (idempotency)
        processedMessageRepository.save(new ProcessedMessage(messageKey));

        logger.info("Report generation COMPLETED for job {}", event.jobId());
    }
}
