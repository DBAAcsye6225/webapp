package com.csye6225.webapp.service;

import com.csye6225.webapp.dto.SyllabusResponse;
import com.csye6225.webapp.entity.Course;
import com.csye6225.webapp.entity.Syllabus;
import com.csye6225.webapp.repository.CourseRepository;
import com.csye6225.webapp.repository.SyllabusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class SyllabusService {

    private static final Logger logger = LoggerFactory.getLogger(SyllabusService.class);

    @Autowired
    private SyllabusRepository syllabusRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private S3Service s3Service;

    /**
     * Upload a syllabus for a course
     */
    @Transactional
    public SyllabusResponse uploadSyllabus(UUID courseId, MultipartFile file) throws IOException {
        // 1. Verify course exists
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    logger.warn("Syllabus upload rejected because courseId={} was not found", courseId);
                    return new RuntimeException("Course not found");
                });

        // 2. Check if syllabus already exists
        if (syllabusRepository.existsByCourseId(courseId.toString())) {
            logger.warn("Syllabus upload rejected because courseId={} already has a syllabus", courseId);
            throw new IllegalArgumentException("A syllabus already exists for this course. Delete it before uploading a new one.");
        }

        // 3. Validate file
        if (file == null || file.isEmpty()) {
            logger.warn("Syllabus upload rejected because the file was null or empty for courseId={}", courseId);
            throw new IllegalArgumentException("File must not be null or empty");
        }

        // 4. Generate S3 object key
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "syllabus";
        }
        String objectKey = courseId + "/" + UUID.randomUUID() + "/" + originalFileName;

        // 5. Upload to S3
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        String url;
        try {
            url = s3Service.uploadFile(objectKey, file.getBytes(), contentType);
        } catch (RuntimeException e) {
            logger.error("Failed to upload syllabus content to S3 for courseId={} and objectKey={}", courseId, objectKey, e);
            throw e;
        }

        // 6. Create Syllabus entity
        Syllabus syllabus = new Syllabus();
        syllabus.setCourseId(courseId.toString());
        syllabus.setFileName(originalFileName);
        syllabus.setS3BucketName(s3Service.getBucketName());
        syllabus.setS3ObjectKey(objectKey);
        syllabus.setContentType(contentType);
        syllabus.setFileSize(file.getSize());
        syllabus.setUrl(url);

        // 7. Save to database
        try {
            Syllabus savedSyllabus = syllabusRepository.save(syllabus);

            // 8. Update course's hasSyllabus flag
            course.setHasSyllabus(true);
            courseRepository.save(course);

            logger.info("Uploaded syllabus {} for courseId={}", savedSyllabus.getId(), courseId);
            return mapToResponse(savedSyllabus);
        } catch (RuntimeException e) {
            logger.error("Failed to persist syllabus metadata for courseId={}", courseId, e);
            throw e;
        }
    }

    /**
     * Get syllabus for a course
     */
    public SyllabusResponse getSyllabus(UUID courseId) {
        // 1. Verify course exists
        courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    logger.warn("Syllabus retrieval rejected because courseId={} was not found", courseId);
                    return new RuntimeException("Course not found");
                });

        // 2. Find syllabus by courseId
        Syllabus syllabus = syllabusRepository.findByCourseId(courseId.toString())
                .orElseThrow(() -> {
                    logger.warn("Syllabus retrieval rejected because no syllabus exists for courseId={}", courseId);
                    return new RuntimeException("No syllabus found for this course");
                });

        logger.info("Retrieved syllabus {} for courseId={}", syllabus.getId(), courseId);

        return mapToResponse(syllabus);
    }

    /**
     * Delete syllabus for a course
     */
    @Transactional
    public void deleteSyllabus(UUID courseId) {
        // 1. Verify course exists
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    logger.warn("Syllabus deletion rejected because courseId={} was not found", courseId);
                    return new RuntimeException("Course not found");
                });

        // 2. Find syllabus by courseId
        Syllabus syllabus = syllabusRepository.findByCourseId(courseId.toString())
                .orElseThrow(() -> {
                    logger.warn("Syllabus deletion rejected because no syllabus exists for courseId={}", courseId);
                    return new RuntimeException("No syllabus found for this course");
                });

        // 3. Delete file from S3
        try {
            s3Service.deleteFile(syllabus.getS3ObjectKey());
        } catch (RuntimeException e) {
            logger.error("Failed to delete syllabus content from S3 for courseId={} and objectKey={}", courseId, syllabus.getS3ObjectKey(), e);
            throw e;
        }

        // 4. Delete syllabus record from database
        try {
            syllabusRepository.delete(syllabus);

            // 5. Update course's hasSyllabus flag
            course.setHasSyllabus(false);
            courseRepository.save(course);
            logger.info("Deleted syllabus {} for courseId={}", syllabus.getId(), courseId);
        } catch (RuntimeException e) {
            logger.error("Failed to delete syllabus metadata for courseId={}", courseId, e);
            throw e;
        }
    }

    /**
     * Convert Syllabus entity to SyllabusResponse DTO
     */
    private SyllabusResponse mapToResponse(Syllabus syllabus) {
        return new SyllabusResponse(
            syllabus.getId(),
            syllabus.getCourseId(),
            syllabus.getFileName(),
            syllabus.getS3BucketName(),
            syllabus.getS3ObjectKey(),
            syllabus.getContentType(),
            syllabus.getFileSize(),
            syllabus.getUrl(),
            syllabus.getDateCreated(),
            syllabus.getDateUpdated()
        );
    }
}
