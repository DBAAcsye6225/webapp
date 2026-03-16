package com.csye6225.webapp.controller;

import com.csye6225.webapp.dto.ErrorResponse;
import com.csye6225.webapp.dto.SyllabusResponse;
import com.csye6225.webapp.service.SyllabusService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/v1/courses/{course_id}/syllabus")
public class SyllabusController {

    private static final Logger logger = LoggerFactory.getLogger(SyllabusController.class);

    @Autowired
    private SyllabusService syllabusService;

    /**
     * POST /v1/courses/{course_id}/syllabus — Upload syllabus file
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadSyllabus(
            @PathVariable("course_id") String courseId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {
        logger.info("Received {} request for {} with courseId={}", request.getMethod(), request.getRequestURI(), courseId);
        try {
            UUID id = UUID.fromString(courseId);

            // Validate file
            if (file == null || file.isEmpty()) {
                logger.warn("Rejected syllabus upload for courseId={} because the file was missing or empty", courseId);
                ErrorResponse error = new ErrorResponse(
                    "Bad Request",
                    "File must not be null or empty",
                    request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            SyllabusResponse response = syllabusService.uploadSyllabus(id, file);
            logger.info("Uploaded syllabus for courseId={} with syllabusId={}", courseId, response.getId());
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/v1/courses/" + courseId + "/syllabus")
                    .body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Rejected syllabus upload for courseId={}: {}", courseId, e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                ErrorResponse error = new ErrorResponse("Conflict", e.getMessage(), request.getRequestURI());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }
            // Invalid UUID format
            ErrorResponse error = new ErrorResponse("Not Found", "Course not found", request.getRequestURI());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Course not found")) {
                logger.warn("Course {} was not found during syllabus upload", courseId);
                ErrorResponse error = new ErrorResponse("Not Found", "Course not found", request.getRequestURI());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            logger.error("Unexpected runtime error uploading syllabus for courseId={}", courseId, e);
            ErrorResponse error = new ErrorResponse("Internal Server Error", "Error uploading syllabus", request.getRequestURI());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            logger.error("Unexpected error uploading syllabus for courseId={}", courseId, e);
            ErrorResponse error = new ErrorResponse("Internal Server Error", "Error uploading syllabus", request.getRequestURI());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /v1/courses/{course_id}/syllabus — Get syllabus metadata
     */
    @GetMapping
    public ResponseEntity<?> getSyllabus(
            @PathVariable("course_id") String courseId,
            HttpServletRequest request) {
        logger.info("Received {} request for {} with courseId={}", request.getMethod(), request.getRequestURI(), courseId);
        try {
            UUID id = UUID.fromString(courseId);
            SyllabusResponse response = syllabusService.getSyllabus(id);
            logger.info("Retrieved syllabus metadata for courseId={} with syllabusId={}", courseId, response.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Rejected syllabus retrieval for courseId={}: {}", courseId, e.getMessage());
            ErrorResponse error = new ErrorResponse("Not Found", "Course not found", request.getRequestURI());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Course not found")) {
                logger.warn("Course {} was not found during syllabus retrieval", courseId);
                ErrorResponse error = new ErrorResponse("Not Found", "Course not found", request.getRequestURI());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            if (e.getMessage() != null && e.getMessage().contains("No syllabus found")) {
                logger.warn("No syllabus exists for courseId={}", courseId);
                ErrorResponse error = new ErrorResponse("Not Found", e.getMessage(), request.getRequestURI());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            logger.error("Unexpected runtime error retrieving syllabus for courseId={}", courseId, e);
            ErrorResponse error = new ErrorResponse("Internal Server Error", "Error retrieving syllabus", request.getRequestURI());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * DELETE /v1/courses/{course_id}/syllabus — Delete syllabus
     */
    @DeleteMapping
    public ResponseEntity<?> deleteSyllabus(
            @PathVariable("course_id") String courseId,
            HttpServletRequest request) {
        logger.info("Received {} request for {} with courseId={}", request.getMethod(), request.getRequestURI(), courseId);
        try {
            UUID id = UUID.fromString(courseId);
            syllabusService.deleteSyllabus(id);
            logger.info("Deleted syllabus for courseId={}", courseId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Rejected syllabus deletion for courseId={}: {}", courseId, e.getMessage());
            ErrorResponse error = new ErrorResponse("Not Found", "Course not found", request.getRequestURI());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Course not found")) {
                logger.warn("Course {} was not found during syllabus deletion", courseId);
                ErrorResponse error = new ErrorResponse("Not Found", "Course not found", request.getRequestURI());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            if (e.getMessage() != null && e.getMessage().contains("No syllabus found")) {
                logger.warn("No syllabus exists for courseId={} during deletion", courseId);
                ErrorResponse error = new ErrorResponse("Not Found", e.getMessage(), request.getRequestURI());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            logger.error("Unexpected runtime error deleting syllabus for courseId={}", courseId, e);
            ErrorResponse error = new ErrorResponse("Internal Server Error", "Error deleting syllabus", request.getRequestURI());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
