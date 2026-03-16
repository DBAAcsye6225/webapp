package com.csye6225.webapp.service;

import com.csye6225.webapp.dto.CourseCreateRequest;
import com.csye6225.webapp.dto.CourseResponse;
import com.csye6225.webapp.dto.CourseUpdateRequest;
import com.csye6225.webapp.entity.Course;
import com.csye6225.webapp.repository.CourseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private static final Logger logger = LoggerFactory.getLogger(CourseService.class);

    @Autowired
    private CourseRepository courseRepository;

    /**
     * Create a new course
     */
    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request) {
        // Check for duplicate department_code + number
        if (courseRepository.existsByDepartmentCodeAndNumber(request.getDepartmentCode(), request.getNumber())) {
            logger.warn("Course creation rejected because departmentCode={} and number={} already exist",
                    request.getDepartmentCode(), request.getNumber());
            throw new IllegalArgumentException("A course with department_code '" + request.getDepartmentCode()
                    + "' and number '" + request.getNumber() + "' already exists");
        }

        // Map request to entity
        Course course = new Course();
        course.setDepartmentCode(request.getDepartmentCode());
        course.setNumber(request.getNumber());
        course.setTitle(request.getTitle());
        course.setCreditHours(request.getCreditHours());
        course.setClassification(request.getClassification());
        course.setDescription(request.getDescription());
        course.setPrerequisites(request.getPrerequisites());

        // Save course
        try {
            Course savedCourse = courseRepository.save(course);
            logger.info("Created course {} for departmentCode={} and number={}",
                savedCourse.getId(), savedCourse.getDepartmentCode(), savedCourse.getNumber());
            return mapToResponse(savedCourse);
        } catch (RuntimeException e) {
            logger.error("Failed to create course for departmentCode={} and number={}",
                request.getDepartmentCode(), request.getNumber(), e);
            throw e;
        }
    }

    /**
     * Get all courses ordered by department_code and number
     */
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAllByOrderByDepartmentCodeAscNumberAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a course by ID
     */
    public CourseResponse getCourseById(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    logger.warn("Course lookup failed because courseId={} was not found", courseId);
                    return new RuntimeException("Course not found");
                });
        return mapToResponse(course);
    }

    /**
     * Find course entity by ID
     */
    public Optional<Course> findById(UUID courseId) {
        return courseRepository.findById(courseId);
    }

    /**
     * Update a course (only non-null fields)
     */
    @Transactional
    public CourseResponse updateCourse(UUID courseId, CourseUpdateRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    logger.warn("Course update rejected because courseId={} was not found", courseId);
                    return new RuntimeException("Course not found");
                });

        boolean updated = false;

        if (request.getTitle() != null) {
            course.setTitle(request.getTitle());
            updated = true;
        }

        if (request.getCreditHours() != null) {
            course.setCreditHours(request.getCreditHours());
            updated = true;
        }

        if (request.getClassification() != null) {
            course.setClassification(request.getClassification());
            updated = true;
        }

        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
            updated = true;
        }

        if (request.getPrerequisites() != null) {
            course.setPrerequisites(request.getPrerequisites());
            updated = true;
        }

        if (updated) {
            try {
                course = courseRepository.save(course);
                logger.info("Updated course {} successfully", courseId);
            } catch (RuntimeException e) {
                logger.error("Failed to update course {}", courseId, e);
                throw e;
            }
        }

        return mapToResponse(course);
    }

    /**
     * Delete a course by ID
     */
    @Transactional
    public void deleteCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    logger.warn("Course deletion rejected because courseId={} was not found", courseId);
                    return new RuntimeException("Course not found");
                });

        // If course has a syllabus, it must be deleted first
        if (Boolean.TRUE.equals(course.getHasSyllabus())) {
            logger.warn("Course deletion rejected because courseId={} still has a syllabus attached", courseId);
            throw new IllegalStateException("Cannot delete course: syllabus must be deleted first");
        }

        try {
            courseRepository.delete(course);
            logger.info("Deleted course {} successfully", courseId);
        } catch (RuntimeException e) {
            logger.error("Failed to delete course {}", courseId, e);
            throw e;
        }
    }

    /**
     * Convert Course entity to CourseResponse DTO
     */
    public CourseResponse mapToResponse(Course course) {
        return new CourseResponse(
            course.getId(),
            course.getDepartmentCode(),
            course.getNumber(),
            course.getTitle(),
            course.getCreditHours(),
            course.getClassification(),
            course.getDescription(),
            course.getPrerequisites(),
            course.getHasSyllabus(),
            course.getDateCreated(),
            course.getDateUpdated()
        );
    }
}
