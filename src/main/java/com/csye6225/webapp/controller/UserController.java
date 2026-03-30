package com.csye6225.webapp.controller;

import com.csye6225.webapp.dto.ErrorResponse;
import com.csye6225.webapp.dto.UserCreateRequest;
import com.csye6225.webapp.dto.UserResponse;
import com.csye6225.webapp.dto.UserUpdateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.csye6225.webapp.entity.User;
import com.csye6225.webapp.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/v1/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Create User (POST) - Keep existing logic
    @PostMapping
    public ResponseEntity<?> createUser(
            @Valid @RequestBody UserCreateRequest request,
            HttpServletRequest httpRequest) {
        logger.info("Received {} request for {} with username={}", httpRequest.getMethod(), httpRequest.getRequestURI(), request.getUsername());
        try {
            UserResponse response = userService.createUser(request);
            logger.info("Created user account for username={}", response.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).header("Location", "/v1/user/self").body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Rejected user creation for username={}: {}", request.getUsername(), e.getMessage());
            ErrorResponse error = new ErrorResponse("Conflict", e.getMessage(), httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        } catch (Exception e) {
            logger.error("Unexpected error creating user for username={}", request.getUsername(), e);
            ErrorResponse error = new ErrorResponse("Internal Server Error", "Error creating user", httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    // Get User (GET) - Keep existing logic
    @GetMapping("/self")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest httpRequest) {
        logger.info("Received {} request for {}", httpRequest.getMethod(), httpRequest.getRequestURI());
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
            logger.info("Retrieved authenticated user details for username={}", username);
            return ResponseEntity.ok(userService.mapToResponse(user));
        } catch (Exception e) {
            logger.warn("Unable to retrieve authenticated user for {}: {}", httpRequest.getRequestURI(), e.getMessage());
            ErrorResponse error = new ErrorResponse("Validation Error", "User account not found", httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    // Update User (PUT) - Logic Corrected Here
    @PutMapping("/self")
    public ResponseEntity<?> updateCurrentUser(
            @RequestBody String requestBody,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            HttpServletRequest httpRequest) {
        logger.info("Received {} request for {}", httpRequest.getMethod(), httpRequest.getRequestURI());
        
        // 1. Check Content-Type
        if (contentType == null || !contentType.contains("application/json")) {
            logger.warn("Rejected user update for {} due to unsupported content type: {}", httpRequest.getRequestURI(), contentType);
            ErrorResponse error = new ErrorResponse("Unsupported Media Type", "Content-Type must be application/json", httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
        }
        
        try {
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            
            // 2. Check for empty body or empty JSON {}
            // This matches the "Bad Request" requirement for empty body
            if (jsonNode.isEmpty()) {
                logger.warn("Rejected user update for {} because the request body was empty", httpRequest.getRequestURI());
                ErrorResponse error = new ErrorResponse(
                    "Bad Request", 
                    "Request body must contain at least one field to update", 
                    httpRequest.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // 3. Define Allowed Fields
            Set<String> allowedFields = new HashSet<>();
            allowedFields.add("first_name");
            allowedFields.add("last_name");
            allowedFields.add("password");
            
            // 4. Check for Disallowed Fields (Logic Priority Changed)
            // We iterate immediately. If we find 'username', we error out immediately.
            // This fixes the issue where it complained about "no valid fields" instead of "illegal field".
            Iterator<String> fieldNames = jsonNode.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                if (!allowedFields.contains(field)) {
                    logger.warn("Rejected user update for {} because field {} is immutable", httpRequest.getRequestURI(), field);
                    // This returns the specific error you wanted: "Field 'username' cannot be updated"
                    ErrorResponse error = new ErrorResponse(
                        "Bad Request", 
                        "Field '" + field + "' cannot be updated", 
                        httpRequest.getRequestURI()
                    );
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }
            }
            
            // 5. Perform Update
            // If we reached here, it means the JSON is not empty AND all fields are valid allowed fields.
            UserUpdateRequest updateRequest = objectMapper.treeToValue(jsonNode, UserUpdateRequest.class);
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            userService.updateUser(user, updateRequest);
            logger.info("Updated authenticated user profile for username={}", username);
            
            // Return 204 No Content
            return ResponseEntity.noContent().build();
            
        } catch (RuntimeException e) {
            logger.warn("Failed to update authenticated user for {}: {}", httpRequest.getRequestURI(), e.getMessage());
            ErrorResponse error = new ErrorResponse("Bad Request", "Invalid JSON format", httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            if (e instanceof JsonProcessingException) {
                logger.warn("Rejected user update for {} due to invalid JSON", httpRequest.getRequestURI());
            } else {
                logger.error("Unexpected error updating authenticated user for {}", httpRequest.getRequestURI(), e);
            }
            ErrorResponse error = new ErrorResponse("Bad Request", "Invalid JSON format", httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/validateEmail")
    public ResponseEntity<?> validateEmail(
            @RequestParam("email") String email,
            @RequestParam("token") String token,
            HttpServletRequest httpRequest) {
        logger.info("Received {} request for {} with email={}", httpRequest.getMethod(), httpRequest.getRequestURI(), email);

        String result = userService.verifyEmail(email, token);
        Map<String, String> response = new LinkedHashMap<>();
        response.put("message", result);

        if ("Email verified successfully".equals(result) || "Already verified".equals(result)) {
            return ResponseEntity.ok(response);
        }

        ErrorResponse error = new ErrorResponse("Bad Request", result, httpRequest.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}