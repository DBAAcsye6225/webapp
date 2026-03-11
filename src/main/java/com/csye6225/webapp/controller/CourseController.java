package com.csye6225.webapp.controller;

import com.csye6225.webapp.dto.CourseCreateRequest;
import com.csye6225.webapp.dto.CourseResponse;
import com.csye6225.webapp.dto.CourseUpdateRequest;
import com.csye6225.webapp.dto.ErrorResponse;
import com.csye6225.webapp.service.CourseService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Validator validator;

    /**
     * POST /v1/courses — Create a new course
     */
    @PostMapping
    public ResponseEntity<?> createCourse(
            @Valid @RequestBody CourseCreateRequest request,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            HttpServletRequest httpRequest) {

        // Check Content-Type
        if (contentType == null || !contentType.contains("application/json")) {
            ErrorResponse error = new ErrorResponse("Unsupported Media Type",
                    "Content-Type must be application/json", httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
        }

        try {
            CourseResponse response = courseService.createCourse(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/v1/courses/" + response.getId())
                    .body(response);
        } catch (IllegalArgumentException e) {
            ErrorResponse error = new ErrorResponse("Conflict", e.getMessage(), httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse("Internal Server Error",
                    "Error creating course", httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /v1/courses — List all courses
     */
    @GetMapping
    public ResponseEntity<?> getAllCourses() {
        List<CourseResponse> courses = courseService.getAllCourses();
        return ResponseEntity.ok(courses);
    }

    /**
     * GET /v1/courses/{course_id} — Get a single course
     */
    @GetMapping("/{course_id}")
    public ResponseEntity<?> getCourseById(
            @PathVariable("course_id") UUID courseId,
            HttpServletRequest httpRequest) {
        try {
            CourseResponse response = courseService.getCourseById(courseId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse("Not Found",
                    "Course not found", httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * PUT /v1/courses/{course_id} — Update a course (partial update)
     */
    @PutMapping("/{course_id}")
    public ResponseEntity<?> updateCourse(
            @PathVariable("course_id") UUID courseId,
            @RequestBody String requestBody,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            HttpServletRequest httpRequest) {

        // Check Content-Type
        if (contentType == null || !contentType.contains("application/json")) {
            ErrorResponse error = new ErrorResponse("Unsupported Media Type",
                    "Content-Type must be application/json", httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(requestBody);

            // Check for empty body
            if (jsonNode.isEmpty()) {
                ErrorResponse error = new ErrorResponse("Bad Request",
                        "Request body must contain at least one field to update",
                        httpRequest.getRequestURI());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Define allowed (mutable) fields
            Set<String> allowedFields = new HashSet<>();
            allowedFields.add("title");
            allowedFields.add("credit_hours");
            allowedFields.add("classification");
            allowedFields.add("description");
            allowedFields.add("prerequisites");

            // Check for immutable / disallowed fields
            Iterator<String> fieldNames = jsonNode.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                if (!allowedFields.contains(field)) {
                    ErrorResponse error = new ErrorResponse("Bad Request",
                            "Field '" + field + "' cannot be updated",
                            httpRequest.getRequestURI());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }
            }

            // Parse and validate update request
            CourseUpdateRequest updateRequest = objectMapper.treeToValue(jsonNode, CourseUpdateRequest.class);

            // Manually invoke Bean Validation (bypassed since we didn't use @Valid)
            Set<ConstraintViolation<CourseUpdateRequest>> violations = validator.validate(updateRequest);
            if (!violations.isEmpty()) {
                String errorMessage = violations.iterator().next().getMessage();
                ErrorResponse error = new ErrorResponse("Validation Error",
                        errorMessage, httpRequest.getRequestURI());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            CourseResponse response = courseService.updateCourse(courseId, updateRequest);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Course not found")) {
                ErrorResponse error = new ErrorResponse("Not Found",
                        "Course not found", httpRequest.getRequestURI());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            ErrorResponse error = new ErrorResponse("Bad Request",
                    e.getMessage(), httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse("Bad Request",
                    "Invalid JSON format", httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * DELETE /v1/courses/{course_id} — Delete a course
     */
    @DeleteMapping("/{course_id}")
    public ResponseEntity<?> deleteCourse(
            @PathVariable("course_id") UUID courseId,
            HttpServletRequest httpRequest) {
        try {
            courseService.deleteCourse(courseId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e instanceof IllegalStateException) {
                // Course has syllabus attached — must delete syllabus first
                ErrorResponse error = new ErrorResponse("Conflict",
                        e.getMessage(), httpRequest.getRequestURI());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }
            // Course not found
            ErrorResponse error = new ErrorResponse("Not Found",
                    "Course not found", httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}
