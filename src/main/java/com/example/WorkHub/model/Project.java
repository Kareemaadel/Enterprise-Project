package com.example.WorkHub.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @org.hibernate.annotations.TenantId
    @Convert(converter = com.example.WorkHub.tenant.UuidStringConverter.class)
    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    public Project() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Tenant getTenant() {
        return tenant;
    }

    // No public setter for tenantId to prevent manual overriding
    public String getTenantId() {
        return tenantId;
    }
}
