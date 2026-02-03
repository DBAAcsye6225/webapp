package com.csye6225.webapp.config;

import com.csye6225.webapp.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        
        response.setContentType("application/json;charset=UTF-8");

        String requestUri = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        if (requestUri == null) {
            requestUri = request.getRequestURI();
        }

        ErrorResponse errorResponse;

        // Recursively check the cause chain to unwrap the exception
        if (isCausedBy(authException, UsernameNotFoundException.class)) {
            // Case 404: User not found
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            errorResponse = new ErrorResponse("Not Found", "User account not found", requestUri);
        } else if (isCausedBy(authException, DisabledException.class)) {
            // Case 403: Account not verified
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            errorResponse = new ErrorResponse("Forbidden", "Account has not been verified. Please check your email.", requestUri);
        } else {
            // Case 401: Wrong password or other auth failures (InsufficientAuthenticationException falls here)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Basic realm=\"Access to user account\"");
            errorResponse = new ErrorResponse("Unauthorized", "Authentication credentials are missing or invalid", requestUri);
        }

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    // Helper method: Check if the exception chain contains a specific type of exception
    private boolean isCausedBy(Throwable throwable, Class<? extends Throwable> targetType) {
        while (throwable != null) {
            if (targetType.isInstance(throwable)) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }
}