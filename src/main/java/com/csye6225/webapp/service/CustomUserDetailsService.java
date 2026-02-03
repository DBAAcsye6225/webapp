package com.csye6225.webapp.service;

import com.csye6225.webapp.entity.User;
import com.csye6225.webapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Fetch user from database
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        // 2. Get the verification status dynamically from the database entity
        // If user.isVerified() is true -> Login allowed (200 OK)
        // If user.isVerified() is false -> Login denied (403 Forbidden via DisabledException)
        boolean isEnabled = user.isVerified();
        
        // 3. Return Spring Security User object
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            isEnabled, // enabled: maps to the 'verified' column
            true, // accountNonExpired
            true, // credentialsNonExpired
            true, // accountNonLocked
            new ArrayList<>() // authorities (empty for now)
        );
    }
}