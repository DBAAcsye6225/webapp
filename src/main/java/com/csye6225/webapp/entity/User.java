package com.csye6225.webapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonProperty("id")
    private UUID id;
    
    @Email
    @NotBlank
    @Column(name = "username", unique = true, nullable = false)
    @JsonProperty("username")
    private String username;
    
    @NotBlank
    @Column(name = "password", nullable = false)
    // We usually don't serialize password to JSON, but keeping it as is based on your previous code
    private String password;
    
    @NotBlank
    @Column(name = "first_name", nullable = false)
    @JsonProperty("first_name")
    private String firstName;
    
    @NotBlank
    @Column(name = "last_name", nullable = false)
    @JsonProperty("last_name")
    private String lastName;

    // --- NEW FIELD FOR 403 TESTING ---
    // This column determines if the user is enabled/verified.
    @Column(name = "verified", nullable = false)
    private boolean verified;
    
    @Column(name = "account_created", nullable = false, updatable = false)
    @JsonProperty("account_created")
    private LocalDateTime accountCreated;
    
    @Column(name = "account_updated", nullable = false)
    @JsonProperty("account_updated")
    private LocalDateTime accountUpdated;
    
    @PrePersist
    protected void onCreate() {
        accountCreated = LocalDateTime.now();
        accountUpdated = LocalDateTime.now();
        // Default to TRUE so created users can login immediately.
        // Update this to FALSE manually in database to test 403.
        this.verified = true; 
    }
    
    @PreUpdate
    protected void onUpdate() {
        accountUpdated = LocalDateTime.now();
    }
    
    // --- Getters and Setters (Complete) ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    // New Getter/Setter for verified
    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public LocalDateTime getAccountCreated() {
        return accountCreated;
    }

    public void setAccountCreated(LocalDateTime accountCreated) {
        this.accountCreated = accountCreated;
    }

    public LocalDateTime getAccountUpdated() {
        return accountUpdated;
    }

    public void setAccountUpdated(LocalDateTime accountUpdated) {
        this.accountUpdated = accountUpdated;
    }
}