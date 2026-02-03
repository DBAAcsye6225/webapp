package com.csye6225.webapp.service;

import com.csye6225.webapp.dto.UserCreateRequest;
import com.csye6225.webapp.dto.UserResponse;
import com.csye6225.webapp.dto.UserUpdateRequest;
import com.csye6225.webapp.entity.User;
import com.csye6225.webapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    
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
            throw new IllegalArgumentException("A user with this email address already exists");
        }
        
        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // BCrypt encryption
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        
        // Save user
        User savedUser = userRepository.save(user);
        
        // Return response
        return mapToResponse(savedUser);
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
            userRepository.save(user);
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