package com.example.WorkHub.model;

import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;
import java.util.UUID;

@Entity
@Table(schema = "public", name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, unique = true)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @ManyToOne
    // @JoinColumn(name = "tenant_id", nullable = false) this is off for the same
    // reason as project.java
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    public User() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public @Nullable String getPassword() {
        return password;
    }

    public void setPassword(@Nullable String encode) {
        this.password = encode;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setRole(String role){
        this.role = role;
    }

    public String getRole(){return role;}
}
