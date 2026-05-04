package com.example.WorkHub.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "job")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String status;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @org.hibernate.annotations.TenantId
    @Convert(converter = com.example.WorkHub.tenant.UuidStringConverter.class)
    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    public Job() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Tenant getTenant() {
        return tenant;
    }

    // No public setter for tenantId to prevent manual overriding
    public String getTenantId() {
        return tenantId;
    }
}
