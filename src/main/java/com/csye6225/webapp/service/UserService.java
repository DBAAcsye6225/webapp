package com.csye6225.webapp.service;

import com.csye6225.webapp.config.SNSConfig;
import com.csye6225.webapp.dto.UserCreateRequest;
import com.csye6225.webapp.dto.UserResponse;
import com.csye6225.webapp.dto.UserUpdateRequest;
import com.csye6225.webapp.entity.User;
import com.csye6225.webapp.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sns.SnsClient;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SnsClient snsClient;

    @Autowired
    private SNSConfig snsConfig;

    @Autowired
    private ObjectMapper objectMapper;
    
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

            String token = UUID.randomUUID().toString();
            savedUser.setVerificationToken(token);
            savedUser.setVerificationTokenExpiry(LocalDateTime.now().plusMinutes(1));
            savedUser = userRepository.save(savedUser);

            publishVerificationMessage(savedUser);

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

    private void publishVerificationMessage(User user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", user.getUsername());
        payload.put("firstName", user.getFirstName());
        payload.put("lastName", user.getLastName());
        payload.put("token", user.getVerificationToken());
        payload.put("tokenExpiry", user.getVerificationTokenExpiry());

        try {
            String message = objectMapper.writeValueAsString(payload);
            snsConfig.publishMessage(snsClient, snsConfig.getTopicArn(), message);
            logger.info("Published verification SNS message for username={}", user.getUsername());
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize verification SNS payload for username={}", user.getUsername(), e);
        } catch (Exception e) {
            logger.error("Failed to publish verification SNS message for username={}", user.getUsername(), e);
        }
    }

    @Transactional
    public String verifyEmail(String email, String token) {
        Optional<User> userOpt = userRepository.findByUsername(email);
        if (userOpt.isEmpty()) {
            logger.warn("Email verification failed: user not found for email={}", email);
            return "User not found";
        }

        User user = userOpt.get();

        if (user.isVerified()) {
            logger.info("Email verification skipped: already verified for email={}", email);
            return "Already verified";
        }

        if (user.getVerificationToken() == null || !user.getVerificationToken().equals(token)) {
            logger.warn("Email verification failed: invalid token for email={}", email);
            return "Invalid token";
        }

        if (user.getVerificationTokenExpiry() == null || user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            logger.warn("Email verification failed: token expired for email={}", email);
            return "Token expired";
        }

        user.setVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
        logger.info("Email verification succeeded for email={}", email);
        return "Email verified successfully";
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
            user.isVerified(),
            user.getAccountCreated(),
            user.getAccountUpdated()
        );
    }
}