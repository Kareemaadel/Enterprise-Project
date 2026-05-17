package com.example.WorkHub.controller;

import com.example.WorkHub.exception.ResourceNotFoundException;
import com.example.WorkHub.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.Table;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final JdbcTemplate jdbcTemplate;

    public GlobalExceptionHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        String tableName = ex.getEntityClass().getSimpleName().toLowerCase();
        if (ex.getEntityClass().isAnnotationPresent(Table.class)) {
            tableName = ex.getEntityClass().getAnnotation(Table.class).name();
        }

        // Bypassing tenant filter to check for global existence
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, ex.getResourceId());

        if (count != null && count > 0) {
            ApiError error = buildError(
                    HttpStatus.FORBIDDEN,
                    "SECURITY ALERT: This " + ex.getEntityClass().getSimpleName() + " belongs to another tenant and is out of your scope.",
                    request,
                    null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        ApiError error = buildError(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request,
                null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ApiError error = buildError(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                fieldErrors);

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        String message = ex.getMostSpecificCause().getMessage();
        logger.warn("Message not readable: {}", message);

        ApiError error = buildError(
                HttpStatus.BAD_REQUEST,
                "Invalid request body format: " + message,
                request,
                null);

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request) {

        HttpStatusCode statusCode = ex.getStatusCode();
        String message = ex.getReason() != null ? ex.getReason() : "Request failed";

        ApiError error = buildError(
                statusCode,
                message,
                request,
                null);

        return ResponseEntity.status(statusCode).body(error);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) 
            {
                ApiError error = buildError(
                        HttpStatus.CONFLICT,
                        "This resource was modified by another request. Please retry.",
                        request, null);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex,
            HttpServletRequest request) {

        ApiError error = buildError(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource.",
                request,
                null);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        logger.error("Unexpected error occurred at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ApiError error = buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error: " + ex.getMessage(),
                request,
                null);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ApiError buildError(
            HttpStatusCode statusCode,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {

        HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());
        String error = httpStatus != null ? httpStatus.getReasonPhrase() : "Unknown";

        return new ApiError(
                OffsetDateTime.now().toString(),
                statusCode.value(),
                error,
                message,
                request.getRequestURI(),
                fieldErrors);
    }
}