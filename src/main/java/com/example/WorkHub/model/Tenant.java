package com.example.WorkHub.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(schema = "public", name = "tenant")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String plan;

    public Tenant() {
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
    
    public String getPlan() { 
        return plan; 
    }
    
    public void setPlan(String plan) { 
        this.plan = plan; 
    }
}
