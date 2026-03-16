package com.csye6225.webapp.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final MeterRegistry meterRegistry;

    public S3Service(@Autowired(required = false) S3Client s3Client,
                     @Value("${aws.s3.bucket-name:}") String bucketName,
                     MeterRegistry meterRegistry) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.meterRegistry = meterRegistry;
    }

    public String getBucketName() {
        return bucketName;
    }

    /**
     * Upload file to S3.
     * @param objectKey the S3 object key (e.g., "courseId/uuid/filename")
     * @param content the file bytes
     * @param contentType MIME type
     * @return the S3 URL
     */
    public String uploadFile(String objectKey, byte[] content, String contentType) {
        if (s3Client == null) {
            logger.warn("Attempted to upload S3 object {} but S3 is not configured", objectKey);
            throw new IllegalStateException("S3 is not configured");
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();

        try {
            s3Client.putObject(putRequest, RequestBody.fromBytes(content));
            logger.info("Uploaded S3 object {} to bucket {}", objectKey, bucketName);
            return String.format("https://%s.s3.amazonaws.com/%s", bucketName, objectKey);
        } catch (RuntimeException e) {
            logger.error("Failed to upload S3 object {} to bucket {}", objectKey, bucketName, e);
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("s3.call.time", "operation", "putObject"));
        }
    }

    /**
     * Delete file from S3.
     */
    public void deleteFile(String objectKey) {
        if (s3Client == null) {
            logger.warn("Attempted to delete S3 object {} but S3 is not configured", objectKey);
            throw new IllegalStateException("S3 is not configured");
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        try {
            s3Client.deleteObject(deleteRequest);
            logger.info("Deleted S3 object {} from bucket {}", objectKey, bucketName);
        } catch (RuntimeException e) {
            logger.error("Failed to delete S3 object {} from bucket {}", objectKey, bucketName, e);
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("s3.call.time", "operation", "deleteObject"));
        }
    }
}
