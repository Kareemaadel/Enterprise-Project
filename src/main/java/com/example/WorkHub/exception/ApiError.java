package com.example.WorkHub.exception;

import java.util.Map;

/**
 * Standard error response DTO used across the application.
 * This ensures all API errors follow a consistent structure.
 */
public record ApiError(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {
}
