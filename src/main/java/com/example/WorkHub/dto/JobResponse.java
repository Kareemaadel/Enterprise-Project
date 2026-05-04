package com.example.WorkHub.dto;

import java.util.UUID;

/**
 * Response DTO for Job/Report entities.
 * Avoids circular reference issues from serializing JPA entities directly.
 */
public record JobResponse(
        UUID id,
        String status,
        UUID projectId,
        UUID tenantId
) {}
