package com.example.WorkHub.dto;

import java.util.UUID;

/**
 * Event DTO published to Kafka when a report generation is requested.
 * Contains all context needed for the consumer to process the job.
 */
public record ReportRequestEvent(
        UUID jobId,
        UUID projectId,
        UUID tenantId,
        String requestedBy
) {}
