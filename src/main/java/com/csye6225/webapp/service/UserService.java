package com.csye6225.webapp.service;

import com.csye6225.webapp.dto.UserCreateRequest;
import com.csye6225.webapp.dto.UserResponse;
import com.csye6225.webapp.dto.UserUpdateRequest;
import com.csye6225.webapp.entity.User;
import com.csye6225.webapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * Create a new user
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // Check if user already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            logger.warn("User creation rejected because username={} already exists", request.getUsername());
            throw new IllegalArgumentException("A user with this email address already exists");
        }
        
        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // BCrypt encryption
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        
        // Save user
        try {
            User savedUser = userRepository.save(user);
            logger.info("Created user account for username={}", savedUser.getUsername());
            return mapToResponse(savedUser);
        } catch (RuntimeException e) {
            logger.error("Failed to create user account for username={}", request.getUsername(), e);
            throw e;
        }
    }
    
    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Find user by ID
     */
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }
    
    /**
     * Update user information
     */
    @Transactional
    public void updateUser(User user, UserUpdateRequest request) {
        boolean updated = false;
        
        // Only update allowed fields
        if (request.getFirstName() != null && !request.getFirstName().isEmpty()) {
            user.setFirstName(request.getFirstName());
            updated = true;
        }
        
        if (request.getLastName() != null && !request.getLastName().isEmpty()) {
            user.setLastName(request.getLastName());
            updated = true;
        }
        
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword())); // BCrypt encryption
            updated = true;
        }
        
        if (updated) {
            try {
                userRepository.save(user);
                logger.info("Updated user account for username={}", user.getUsername());
            } catch (RuntimeException e) {
                logger.error("Failed to update user account for username={}", user.getUsername(), e);
                throw e;
            }
        }
    }
    
    /**
     * Verify password
     */
    public boolean verifyPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }
    
    /**
     * Convert User entity to UserResponse
     */
    public UserResponse mapToResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.getAccountCreated(),
            user.getAccountUpdated()
        );
    }
}