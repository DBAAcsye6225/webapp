package com.csye6225.webapp.controller;

import com.csye6225.webapp.exception.MetadataUnavailableException;
import com.csye6225.webapp.model.MetadataResponse;
import com.csye6225.webapp.service.AwsMetadataService;
import com.csye6225.webapp.service.CloudPlatformDetector;
import com.csye6225.webapp.service.GcpMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class MetadataController {

    private static final Logger logger = LoggerFactory.getLogger(MetadataController.class);
    
    @Autowired
    private CloudPlatformDetector platformDetector;
    
    @Autowired
    private AwsMetadataService awsMetadataService;
    
    @Autowired
    private GcpMetadataService gcpMetadataService;
    
    @GetMapping("/metadata")
    public ResponseEntity<?> getMetadata(
            HttpServletRequest request,
            @RequestParam(required = false) Map<String, String> params) {
        logger.info("Received {} request for {}", request.getMethod(), request.getRequestURI());
        
        // Validate no query parameters
        if (params != null && !params.isEmpty()) {
            logger.warn("Rejected metadata request for {} because query parameters are not allowed", request.getRequestURI());
            return ResponseEntity
                    .badRequest()
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .body(Map.of("error", "Query parameters not allowed"));
        }
        
        // Check for request body
        try {
            BufferedReader reader = request.getReader();
            String line;
            StringBuilder bodyContent = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                bodyContent.append(line);
            }
            
            if (bodyContent.length() > 0) {
                logger.warn("Rejected metadata request for {} because a request body was provided", request.getRequestURI());
                return ResponseEntity
                        .badRequest()
                        .header("Cache-Control", "no-cache, no-store, must-revalidate")
                        .header("Pragma", "no-cache")
                        .body(Map.of("error", "Request body not allowed"));
            }
        } catch (Exception e) {
            // Ignore errors reading body
        }
        
        try {
            // Detect cloud platform
            String platform = platformDetector.detectPlatform();
            
            if (platform == null) {
                logger.warn("Metadata service unavailable for {} because no supported platform was detected", request.getRequestURI());
                // Not running on a supported cloud platform
                return ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .header("Cache-Control", "no-cache, no-store, must-revalidate")
                        .header("Pragma", "no-cache")
                        .body(Map.of("error", "Metadata service unavailable - not running on supported cloud platform"));
            }
            
            // Get metadata from appropriate service
            MetadataResponse metadata;
            if ("aws".equals(platform)) {
                metadata = awsMetadataService.getMetadata();
            } else if ("gcp".equals(platform)) {
                metadata = gcpMetadataService.getMetadata();
            } else {
                throw new MetadataUnavailableException("Unknown platform: " + platform);
            }
            logger.info("Retrieved metadata successfully for platform={}", platform);
            
            return ResponseEntity
                    .ok()
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .body(metadata);
                    
        } catch (MetadataUnavailableException e) {
                    logger.warn("Metadata request failed for {}: {}", request.getRequestURI(), e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    // Handle non-GET methods
    @RequestMapping(value = "/metadata", method = {
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.DELETE,
            RequestMethod.PATCH,
            RequestMethod.HEAD,
            RequestMethod.OPTIONS
    })
    public ResponseEntity<?> handleInvalidMethods(HttpServletRequest request) {
        logger.warn("Rejected unsupported {} request for {}", request.getMethod(), request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .body(Map.of("error", "Method not allowed"));
    }
}
