package com.csye6225.webapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat; // Import this
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public class UserResponse {
    
    @JsonProperty("id")
    private UUID id;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("verified")
    private boolean verified;
    
    // Fix: Match format 2024-01-15T10:30:00.000Z
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @JsonProperty("account_created")
    private LocalDateTime accountCreated;
    
    // Fix: Match format 2024-01-15T10:30:00.000Z
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @JsonProperty("account_updated")
    private LocalDateTime accountUpdated;
    
    // Constructors
    public UserResponse() {}
    
    public UserResponse(UUID id, String username, String firstName, String lastName,
                       boolean verified, LocalDateTime accountCreated, LocalDateTime accountUpdated) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.verified = verified;
        this.accountCreated = accountCreated;
        this.accountUpdated = accountUpdated;
    }
    
    // Getters and Setters... (Keep existing getters and setters)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public LocalDateTime getAccountCreated() { return accountCreated; }
    public void setAccountCreated(LocalDateTime accountCreated) { this.accountCreated = accountCreated; }
    public LocalDateTime getAccountUpdated() { return accountUpdated; }
    public void setAccountUpdated(LocalDateTime accountUpdated) { this.accountUpdated = accountUpdated; }
}