package com.csye6225.webapp.controller;

import com.csye6225.webapp.entity.HealthCheck;
import com.csye6225.webapp.repository.HealthCheckRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class HealthCheckController {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);
    
    @Autowired
    private HealthCheckRepository healthCheckRepository;
    
    @GetMapping("/healthz")
    public ResponseEntity<Void> healthCheck(
            @RequestBody(required = false) String body,
            HttpServletRequest request) {
        logger.info("Received {} request for {}", request.getMethod(), request.getRequestURI());
        
        // Check if request contains query parameters
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            logger.warn("Rejected health check request with query parameters for {}", request.getRequestURI());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("X-Content-Type-Options", "nosniff")
                    .build();
        }
        
        // Check if request contains payload
        if (body != null && !body.isEmpty()) {
            logger.warn("Rejected health check request with payload for {}", request.getRequestURI());
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
            logger.info("Health check completed successfully for {}", request.getRequestURI());
            
            // Return 200 OK
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("X-Content-Type-Options", "nosniff")
                    .build();
        } catch (Exception e) {
            // Database connection failed, return 503
            logger.error("Health check failed for {}", request.getRequestURI(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("X-Content-Type-Options", "nosniff")
                    .build();
        }
    }
    
    @RequestMapping(value = "/healthz", method = {RequestMethod.POST, RequestMethod.PUT, 
                    RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.OPTIONS})
    public ResponseEntity<Void> healthCheckNotAllowed(HttpServletRequest request) {
        logger.warn("Rejected unsupported {} request for {}", request.getMethod(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .build();
    }
}