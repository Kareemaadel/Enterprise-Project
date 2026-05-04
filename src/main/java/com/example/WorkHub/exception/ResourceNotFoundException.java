package com.example.WorkHub.exception;

import java.util.UUID;

/**
 * Custom exception to indicate a resource was not found in the current context.
 * The GlobalExceptionHandler will intercept this to determine if the resource 
 * exists globally (tenant mismatch) or is truly missing.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    private final Class<?> entityClass;
    private final UUID resourceId;

    public ResourceNotFoundException(Class<?> entityClass, UUID resourceId) {
        super(entityClass.getSimpleName() + " not found");
        this.entityClass = entityClass;
        this.resourceId = resourceId;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public UUID getResourceId() {
        return resourceId;
    }
}
