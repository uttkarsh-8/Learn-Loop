package com.oath2.oath20.service;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class S3Service {
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final AmazonS3 s3Client;
    private final String bucketName;

    public S3Service(@Value("${aws.s3.bucket-name}") String bucketName,
                     @Value("${aws.region}") String region) {
        this.s3Client = AmazonS3ClientBuilder.standard().withRegion(region).build();
        this.bucketName = bucketName;
    }

    public String generatePresignedUrl(String objectKey, HttpMethod httpMethod) {
        try {
            Date expiration = Date.from(Instant.now().plus(Duration.ofMinutes(15)));
            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                    new GeneratePresignedUrlRequest(bucketName, objectKey)
                            .withMethod(httpMethod)
                            .withExpiration(expiration);
            URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
            logger.info("Generated pre-signed URL for object: {}", objectKey);
            return url.toString();
        } catch (AmazonServiceException e) {
            logger.error("AmazonServiceException when generating pre-signed URL for object: {}", objectKey, e);
            throw new RuntimeException("Failed to generate pre-signed URL", e);
        } catch (SdkClientException e) {
            logger.error("SdkClientException when generating pre-signed URL for object: {}", objectKey, e);
            throw new RuntimeException("Failed to generate pre-signed URL", e);
        }
    }

    public String uploadFile(MultipartFile file) {
        String fileKey = generateUniqueFileKey(file.getOriginalFilename());
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            s3Client.putObject(bucketName, fileKey, file.getInputStream(), metadata);
            logger.info("Successfully uploaded file to S3: {}", fileKey);
            return fileKey;
        } catch (IOException e) {
            logger.error("IOException when uploading file to S3", e);
            throw new RuntimeException("Failed to upload file", e);
        } catch (AmazonServiceException e) {
            logger.error("AmazonServiceException when uploading file to S3", e);
            throw new RuntimeException("Failed to upload file", e);
        } catch (SdkClientException e) {
            logger.error("SdkClientException when uploading file to S3", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public void deleteFile(String objectKey) {
        try {
            s3Client.deleteObject(bucketName, objectKey);
            logger.info("Successfully deleted file from S3: {}", objectKey);
        } catch (AmazonServiceException e) {
            logger.error("AmazonServiceException when deleting file from S3: {}", objectKey, e);
            throw new RuntimeException("Failed to delete file", e);
        } catch (SdkClientException e) {
            logger.error("SdkClientException when deleting file from S3: {}", objectKey, e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    private String generateUniqueFileKey(String originalFilename) {
        return UUID.randomUUID().toString() + "-" + originalFilename;
    }
}

