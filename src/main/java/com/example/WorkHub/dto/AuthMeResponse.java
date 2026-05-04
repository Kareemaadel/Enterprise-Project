package com.example.WorkHub.dto;

import java.util.UUID;

public record AuthMeResponse(String email, UUID tenantId, String role) {
}
