package com.csye6225.webapp.controller;

import com.csye6225.webapp.entity.HealthCheck;
import com.csye6225.webapp.repository.HealthCheckRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class HealthCheckController {
    
    @Autowired
    private HealthCheckRepository healthCheckRepository;
    
    @GetMapping("/healthz")
    public ResponseEntity<Void> healthCheck(
            @RequestBody(required = false) String body,
            HttpServletRequest request) {
        
        // Check if request contains query parameters
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("X-Content-Type-Options", "nosniff")
                    .build();
        }
        
        // Check if request contains payload
        if (body != null && !body.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("X-Content-Type-Options", "nosniff")
                    .build();
        }
        
        try {
            // Insert health check record
            HealthCheck healthCheck = new HealthCheck();
            healthCheckRepository.save(healthCheck);
            
            // Return 200 OK
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("X-Content-Type-Options", "nosniff")
                    .build();
        } catch (Exception e) {
            // Database connection failed, return 503
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("X-Content-Type-Options", "nosniff")
                    .build();
        }
    }
    
    @RequestMapping(value = "/healthz", method = {RequestMethod.POST, RequestMethod.PUT, 
                    RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.OPTIONS})
    public ResponseEntity<Void> healthCheckNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .build();
    }
}