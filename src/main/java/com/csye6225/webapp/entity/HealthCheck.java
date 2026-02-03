package com.csye6225.webapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_checks")
public class HealthCheck {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "check_id")
    private Long checkId;
    
    @Column(name = "check_datetime", nullable = false)
    private LocalDateTime checkDatetime;
    
    @PrePersist
    protected void onCreate() {
        checkDatetime = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getCheckId() {
        return checkId;
    }
    
    public void setCheckId(Long checkId) {
        this.checkId = checkId;
    }
    
    public LocalDateTime getCheckDatetime() {
        return checkDatetime;
    }
    
    public void setCheckDatetime(LocalDateTime checkDatetime) {
        this.checkDatetime = checkDatetime;
    }
}